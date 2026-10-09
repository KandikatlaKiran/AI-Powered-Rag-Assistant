package com.example.dto;

import com.example.model.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * API response DTO for document operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private String id;
    private String fileName;
    private DocumentStatus status;
    private int chunkCount;
    private long fileSize;
    private Instant uploadedAt;
    private String errorMessage;
    private String message;
}