package com.example.exception;

/**
 * Thrown when a user uploads a file with an unsupported MIME type or extension.
 */
public class UnsupportedDocumentTypeException extends RuntimeException {

    public UnsupportedDocumentTypeException(String fileType) {
        super("Unsupported document type: " + fileType + ". Accepted types: PDF, DOCX, TXT");
    }
}
