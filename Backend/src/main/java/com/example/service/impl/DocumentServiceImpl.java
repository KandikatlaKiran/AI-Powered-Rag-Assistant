package com.example.service.impl;

import com.example.dto.DocumentResponse;
import com.example.exception.DocumentNotFoundException;
import com.example.exception.DocumentProcessingException;
import com.example.exception.UnsupportedDocumentTypeException;
import com.example.model.DocumentMetadata;
import com.example.model.DocumentStatus;
import com.example.repository.DocumentRepository;
import com.example.service.DocumentService;
import com.example.service.EmbeddingService;
import com.example.util.TextChunker;
import com.example.util.TextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Manages the full document lifecycle: validation -> duplicate cleanup -> ingestion pipeline -> status tracking -> deletion.
 *
 * <p>Document metadata is persisted via {@link DocumentRepository}, ensuring all state survives application restarts.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt");
    private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9._\\-]");

    private final DocumentRepository documentRepository;
    private final EmbeddingService embeddingService;
    private final TextChunker textChunker;

    @Override
    public DocumentResponse uploadDocument(MultipartFile file) {
        log.info("Document upload initiated: '{}' ({} bytes)", file.getOriginalFilename(), file.getSize());

        validateFile(file);

        String originalName = file.getOriginalFilename();
        String sanitizedName = sanitizeFileName(originalName);

        // Duplicate Handling Strategy: Replace existing document.
        // If a file with the same name was previously uploaded, delete its old chunks from ChromaDB
        // and replace its registry entry to avoid orphaned or duplicate vectors.
        handleDuplicateIfPresent(originalName);

        String documentId = UUID.randomUUID().toString();

        DocumentMetadata metadata = DocumentMetadata.builder()
                .id(documentId)
                .originalFileName(originalName)
                .sanitizedFileName(sanitizedName)
                .fileSize(file.getSize())
                .status(DocumentStatus.UPLOADING)
                .uploadedAt(Instant.now())
                .chunkIds(new ArrayList<>())
                .build();

        documentRepository.save(metadata);
        log.info("Document registered with id={} status=UPLOADING", documentId);

        // Run the ingestion pipeline synchronously for guaranteed consistency
        processDocument(documentId, file, sanitizedName);

        return toResponse(documentRepository.findById(documentId).orElse(metadata));
    }

    /**
     * Checks if a document with the same filename already exists.
     * If so, cleanly removes its vectors from ChromaDB and deletes the old record.
     */
    private void handleDuplicateIfPresent(String originalFileName) {
        Optional<DocumentMetadata> existingOpt = documentRepository.findByOriginalFileNameIgnoreCase(originalFileName);
        if (existingOpt.isPresent()) {
            DocumentMetadata existing = existingOpt.get();
            log.info("Duplicate document detected: '{}' (existing id={}). Replacing existing version...", originalFileName, existing.getId());
            try {
                if (existing.getChunkIds() != null && !existing.getChunkIds().isEmpty()) {
                    log.info("Cleaning up {} previous vectors from ChromaDB for duplicate replace", existing.getChunkIds().size());
                    embeddingService.deleteByChunkIds(existing.getChunkIds());
                }
                documentRepository.deleteById(existing.getId());
                log.info("Previous version of '{}' (id={}) removed successfully", originalFileName, existing.getId());
            } catch (Exception e) {
                log.warn("Failed to cleanly delete previous version of duplicate document (id={}): {}", existing.getId(), e.getMessage());
                documentRepository.deleteById(existing.getId());
            }
        }
    }

    protected void processDocument(String documentId, MultipartFile file, String sanitizedName) {
        DocumentMetadata metadata = documentRepository.findById(documentId).orElse(null);
        if (metadata == null) return;

        long uploadPipelineStart = System.currentTimeMillis();

        try {
            // Step 1: Text extraction
            metadata.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(metadata);
            log.info("Text extraction started for document id={}", documentId);
            long extractStart = System.currentTimeMillis();

            String rawText = TextExtractor.extractText(file);
            if (rawText == null || rawText.isBlank()) {
                throw new DocumentProcessingException("No text could be extracted from the document");
            }
            long extractDuration = System.currentTimeMillis() - extractStart;
            log.info("Text extraction completed for document id={} — {} chars extracted in {} ms",
                    documentId, rawText.length(), extractDuration);

            // Step 2: Chunking
            log.info("Chunking started for document id={}", documentId);
            long chunkStart = System.currentTimeMillis();

            Document fullDocument = new Document(rawText);
            List<Document> chunks = textChunker.chunk(fullDocument);
            long chunkDuration = System.currentTimeMillis() - chunkStart;
            log.info("Chunking completed for document id={} — {} chunks produced in {} ms",
                    documentId, chunks.size(), chunkDuration);

            // Step 3: Embed + store in ChromaDB
            log.info("Embedding generation started (nomic-embed-text) for document id={} — {} chunks", documentId, chunks.size());
            long embedStart = System.currentTimeMillis();

            List<String> chunkIds = embeddingService.embedAndStore(documentId, sanitizedName, chunks);
            long embedDuration = System.currentTimeMillis() - embedStart;
            log.info("Embedding + ChromaDB storage completed for document id={} — {} vectors in {} ms",
                    documentId, chunkIds.size(), embedDuration);

            // Step 4: Mark complete and persist
            metadata.setStatus(DocumentStatus.COMPLETED);
            metadata.setChunkCount(chunks.size());
            metadata.setChunkIds(chunkIds);
            metadata.setErrorMessage(null);
            documentRepository.save(metadata);

            long totalDuration = System.currentTimeMillis() - uploadPipelineStart;
            log.info("\n" +
                    "========== UPLOAD PERFORMANCE ==========\n" +
                    "Document: {}\n" +
                    "Text extraction: {} ms\n" +
                    "Chunking: {} ms ({} chunks)\n" +
                    "Embedding (nomic-embed-text) + ChromaDB: {} ms\n" +
                    "Total upload pipeline: {} ms\n" +
                    "=========================================",
                    sanitizedName, extractDuration, chunkDuration, chunks.size(), embedDuration, totalDuration);

        } catch (IOException e) {
            log.error("Text extraction failed for document id={}: {}", documentId, e.getMessage(), e);
            markFailed(documentId, "Failed to extract text: " + e.getMessage());
        } catch (DocumentProcessingException e) {
            log.error("Processing error for document id={}: {}", documentId, e.getMessage());
            markFailed(documentId, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing document id={}: {}", documentId, e.getMessage(), e);
            markFailed(documentId, "An unexpected error occurred during processing: " + e.getMessage());
        }
    }

    @Override
    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentResponse getDocument(String documentId) {
        DocumentMetadata metadata = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        return toResponse(metadata);
    }

    @Override
    public void deleteDocument(String documentId) {
        DocumentMetadata metadata = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        log.info("Document deletion requested for id={} fileName='{}'", documentId, metadata.getSanitizedFileName());
        metadata.setStatus(DocumentStatus.DELETING);
        documentRepository.save(metadata);

        try {
            List<String> idsToDelete = metadata.getChunkIds();
            // Fallback: If chunkIds list is empty but chunkCount is known, generate expected IDs
            if ((idsToDelete == null || idsToDelete.isEmpty()) && metadata.getChunkCount() > 0) {
                idsToDelete = new ArrayList<>();
                for (int i = 0; i < metadata.getChunkCount(); i++) {
                    idsToDelete.add(documentId + "-chunk-" + i);
                }
            }

            if (idsToDelete != null && !idsToDelete.isEmpty()) {
                log.info("Deleting {} vectors from ChromaDB for document id={}", idsToDelete.size(), documentId);
                embeddingService.deleteByChunkIds(idsToDelete);
            }

            documentRepository.deleteById(documentId);
            log.info("Document and all associated vectors deleted successfully: id={}", documentId);

        } catch (Exception e) {
            log.error("Failed to cleanly delete vectors for document id={}: {}", documentId, e.getMessage(), e);
            documentRepository.deleteById(documentId);
            throw new DocumentProcessingException("Document deleted from registry but vector cleanup encountered an issue: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("File must have a valid name");
        }

        String extension = getExtension(originalName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedDocumentTypeException(extension);
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            // Allow text/plain variants
            if (!contentType.startsWith("text/")) {
                throw new UnsupportedDocumentTypeException(contentType);
            }
        }
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null) return "unknown-" + UUID.randomUUID();
        return UNSAFE_FILENAME_CHARS.matcher(originalName.trim()).replaceAll("_");
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0 && dot < fileName.length() - 1) ? fileName.substring(dot + 1) : "";
    }

    private void markFailed(String documentId, String errorMessage) {
        DocumentMetadata metadata = documentRepository.findById(documentId).orElse(null);
        if (metadata != null) {
            metadata.setStatus(DocumentStatus.FAILED);
            metadata.setErrorMessage(errorMessage);
            documentRepository.save(metadata);
        }
    }

    private DocumentResponse toResponse(DocumentMetadata metadata) {
        return DocumentResponse.builder()
                .id(metadata.getId())
                .fileName(metadata.getOriginalFileName())
                .status(metadata.getStatus())
                .chunkCount(metadata.getChunkCount())
                .fileSize(metadata.getFileSize())
                .uploadedAt(metadata.getUploadedAt())
                .errorMessage(metadata.getErrorMessage())
                .message(statusMessage(metadata.getStatus()))
                .build();
    }

    private String statusMessage(DocumentStatus status) {
        return switch (status) {
            case UPLOADING -> "Uploading...";
            case PROCESSING -> "Processing document...";
            case COMPLETED -> "Ready";
            case FAILED -> "Processing failed";
            case DELETING -> "Deleting...";
        };
    }
}
