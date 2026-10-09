package com.example.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centralised exception handler. Returns structured {@link ErrorResponse} JSON
 * for all error cases. Internal stack traces are never forwarded to clients.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(
            DocumentNotFoundException ex, HttpServletRequest request) {
        log.warn("Document not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Document Not Found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnsupportedDocumentTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedType(
            UnsupportedDocumentTypeException ex, HttpServletRequest request) {
        log.warn("Unsupported document type: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Unsupported Document Type", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ErrorResponse> handleDocumentProcessing(
            DocumentProcessingException ex, HttpServletRequest request) {
        log.error("Document processing failed: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Document Processing Error", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RagException.class)
    public ResponseEntity<ErrorResponse> handleRag(
            RagException ex, HttpServletRequest request) {
        log.error("RAG pipeline error: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "RAG Pipeline Error", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "File Too Large",
                "The uploaded file exceeds the maximum allowed size of 50MB.", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Error", details, request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String error, String message, String path) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
