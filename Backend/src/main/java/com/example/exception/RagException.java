package com.example.exception;

/**
 * Thrown when the RAG pipeline (similarity search, LLM generation) encounters an error.
 */
public class RagException extends RuntimeException {

    public RagException(String message) {
        super(message);
    }

    public RagException(String message, Throwable cause) {
        super(message, cause);
    }
}
