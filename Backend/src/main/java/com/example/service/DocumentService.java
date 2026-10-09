package com.example.service;

import com.example.dto.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Document management service. Handles the full document lifecycle:
 * upload, processing pipeline, status tracking, and deletion.
 */
public interface DocumentService {

    /**
     * Accepts a multipart file, validates it, runs the ingestion pipeline
     * (extract → chunk → embed → store), and returns the initial document response.
     *
     * @param file the uploaded multipart file
     * @return document response with id and initial status
     */
    DocumentResponse uploadDocument(MultipartFile file);

    /**
     * Returns all documents currently in the registry.
     */
    List<DocumentResponse> getAllDocuments();

    /**
     * Returns a single document by ID, or throws {@link com.example.exception.DocumentNotFoundException}.
     */
    DocumentResponse getDocument(String documentId);

    /**
     * Deletes a document and all associated vectors from ChromaDB.
     *
     * @param documentId the document UUID
     */
    void deleteDocument(String documentId);
}
