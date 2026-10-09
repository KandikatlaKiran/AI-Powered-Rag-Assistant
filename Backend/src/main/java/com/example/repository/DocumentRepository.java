package com.example.repository;

import com.example.model.DocumentMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for document metadata persistence.
 */
public interface DocumentRepository {

    /**
     * Retrieve all documents ordered by upload timestamp descending.
     */
    List<DocumentMetadata> findAll();

    /**
     * Find a document by its unique UUID.
     */
    Optional<DocumentMetadata> findById(String id);

    /**
     * Find a document by its original filename (case-insensitive) to handle duplicates.
     */
    Optional<DocumentMetadata> findByOriginalFileNameIgnoreCase(String fileName);

    /**
     * Save or update document metadata.
     */
    DocumentMetadata save(DocumentMetadata metadata);

    /**
     * Delete document metadata by UUID.
     */
    void deleteById(String id);

    /**
     * Check if a document exists by UUID.
     */
    boolean existsById(String id);

    /**
     * Return total number of registered documents.
     */
    long count();
}
