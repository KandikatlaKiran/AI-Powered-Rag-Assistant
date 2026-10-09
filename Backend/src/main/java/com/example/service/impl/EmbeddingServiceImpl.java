package com.example.service.impl;

import com.example.service.EmbeddingService;
import com.example.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handles embedding enrichment and vector search operations.
 *
 * <p>Spring AI's {@link org.springframework.ai.vectorstore.VectorStore#add} method
 * automatically calls the configured embedding model (nomic-embed-text via Ollama)
 * before storing into ChromaDB.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final VectorStoreService vectorStoreService;

    @Override
    public List<String> embedAndStore(String documentId, String fileName, List<Document> chunks) {
        log.info("Enriching and storing {} chunks for document '{}' (id={})", chunks.size(), fileName, documentId);

        List<Document> enrichedChunks = new ArrayList<>();
        List<String> chunkIds = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Document original = chunks.get(i);
            String chunkId = documentId + "-chunk-" + i;

            Map<String, Object> metadata = new HashMap<>();
            if (original.getMetadata() != null) {
                original.getMetadata().forEach((k, v) -> {
                    if (k != null && v != null) {
                        metadata.put(k, v);
                    }
                });
            }

            metadata.put("documentId", documentId);
            metadata.put("fileName", fileName);
            metadata.put("documentName", fileName);
            metadata.put("chunkId", chunkId);
            metadata.put("chunkIndex", i);
            metadata.put("source", fileName);
            metadata.put("uploadTimestamp", Instant.now().toString());

            // Ensure no null keys or values exist in metadata
            metadata.values().removeIf(Objects::isNull);

            Document enriched = new Document(chunkId, original.getText(), metadata);
            enrichedChunks.add(enriched);
            chunkIds.add(chunkId);
        }

        log.info("Sending {} enriched chunks to ChromaDB vector store (embeddings via nomic-embed-text)", enrichedChunks.size());
        vectorStoreService.store(enrichedChunks);
        log.info("Embedding generation and ChromaDB storage completed for document '{}'", fileName);

        return chunkIds;
    }

    @Override
    public List<Document> searchSimilar(String question, int maxCandidates, double similarityThreshold) {
        log.info("Searching similar chunks with maxCandidates={}, similarityThreshold={} for question (length={})",
                maxCandidates, similarityThreshold, question.length());
        List<Document> results = vectorStoreService.similaritySearch(question, maxCandidates, similarityThreshold);
        log.info("Similarity search returned {} chunks matching threshold >= {}", results.size(), similarityThreshold);
        return results;
    }

    @Override
    public List<Document> searchSimilar(String question, int topK) {
        log.info("Searching for top-{} similar chunks for question (length={})", topK, question.length());
        List<Document> results = vectorStoreService.similaritySearch(question, topK);
        log.info("Similarity search returned {} results", results.size());
        return results;
    }

    @Override
    public void deleteByChunkIds(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            log.warn("No chunk IDs provided for vector deletion");
            return;
        }
        log.info("Deleting {} chunk vectors from ChromaDB", chunkIds.size());
        vectorStoreService.delete(chunkIds);
        log.info("ChromaDB vector deletion completed for {} IDs", chunkIds.size());
    }
}
