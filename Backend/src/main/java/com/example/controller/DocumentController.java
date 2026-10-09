package com.example.controller;

import com.example.dto.DocumentResponse;
import com.example.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for document management.
 *
 * <pre>
 * POST   /api/documents         — Upload a document
 * GET    /api/documents         — List all documents
 * GET    /api/documents/{id}    — Get document by ID
 * DELETE /api/documents/{id}    — Delete document and its vectors
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Upload a document and start the RAG ingestion pipeline.
     *
     * @param file the multipart file (PDF, DOCX, or TXT)
     * @return 202 Accepted with initial document metadata
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        log.info("POST /api/documents — file='{}'", file.getOriginalFilename());
        DocumentResponse response = documentService.uploadDocument(file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * List all documents currently in the registry.
     *
     * @return 200 OK with list of document responses ordered by upload time (newest first)
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        log.debug("GET /api/documents");
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    /**
     * Get a single document by ID.
     *
     * @param id document UUID
     * @return 200 OK with document metadata, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable String id) {
        log.debug("GET /api/documents/{}", id);
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    /**
     * Delete a document and all its associated ChromaDB vectors.
     *
     * @param id document UUID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        log.info("DELETE /api/documents/{}", id);
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
