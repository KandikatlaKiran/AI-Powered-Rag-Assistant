package com.example.exception;

/**
 * Thrown when a requested document ID does not exist in the registry.
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String documentId) {
        super("Document not found: " + documentId);
    }
}
