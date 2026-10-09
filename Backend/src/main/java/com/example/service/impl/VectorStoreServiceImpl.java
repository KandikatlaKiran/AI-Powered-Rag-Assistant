package com.example.service.impl;

import com.example.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin wrapper over Spring AI's auto-configured {@link VectorStore} (ChromaDB).
 * All calls are delegated directly — this layer exists to provide a mockable
 * seam for testing and to isolate the vector store API from business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final VectorStore vectorStore;

    /** Soft similarity score floor — chunks below this score are excluded as noise. */
    @Value("${app.rag.similarity-threshold:0.15}")
    private double similarityThreshold;

    @Override
    public void store(List<Document> documents) {
        log.debug("Storing {} documents in vector store", documents.size());
        vectorStore.add(documents);
        log.debug("Successfully stored {} documents", documents.size());
    }

    @Override
    public List<Document> similaritySearch(String query, int maxResults, double similarityThreshold) {
        log.debug("Running similarity search with maxResults={}, similarityThreshold={}", maxResults, similarityThreshold);
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(maxResults)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );
    }

    /**
     * Top-K search with the configured soft similarity threshold applied.
     * Ensures noisy / irrelevant chunks are excluded even when topK candidates are fetched.
     */
    @Override
    public List<Document> similaritySearch(String query, int topK) {
        log.debug("Running similarity search with topK={}, similarityThreshold={}", topK, similarityThreshold);
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );
    }

    @Override
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            log.debug("No vector IDs to delete");
            return;
        }
        log.debug("Deleting {} vectors from store", ids.size());
        vectorStore.delete(ids);
        log.debug("Successfully deleted {} vectors", ids.size());
    }
}
