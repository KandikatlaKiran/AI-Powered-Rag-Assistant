package com.example.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Embedding and vector search service.
 * Bridges the document processing pipeline and the vector store.
 */
public interface EmbeddingService {

    /**
     * Enriches Spring AI {@link Document} chunks with full metadata, then stores
     * their embeddings in the vector store.
     *
     * @param documentId unique document UUID
     * @param fileName   sanitized file name for metadata
     * @param chunks     text chunks produced by {@link com.example.util.TextChunker}
     * @return list of chunk IDs stored in the vector store (for deletion later)
     */
    List<String> embedAndStore(String documentId, String fileName, List<Document> chunks);

    /**
     * Embeds the given query and performs a similarity search in the vector store,
     * filtering results by a similarity threshold score.
     *
     * @param question            the user's question
     * @param maxCandidates       maximum number of candidate results to retrieve
     * @param similarityThreshold minimum similarity score threshold (0.0 to 1.0)
     * @return list of relevant {@link Document} chunks
     */
    List<Document> searchSimilar(String question, int maxCandidates, double similarityThreshold);

    /**
     * Embeds the given query and performs a similarity search in the vector store.
     *
     * @param question the user's question
     * @param topK     maximum number of results to return
     * @return the most relevant {@link Document} chunks
     */
    List<Document> searchSimilar(String question, int topK);

    /**
     * Deletes specific vector entries from the vector store by their IDs.
     *
     * @param chunkIds the list of vector IDs to remove
     */
    void deleteByChunkIds(List<String> chunkIds);
}
