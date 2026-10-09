package com.example.exception;

/**
 * Thrown when document text extraction, chunking, or vector storage fails.
 */
public class DocumentProcessingException extends RuntimeException {

    public DocumentProcessingException(String message) {
        super(message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
