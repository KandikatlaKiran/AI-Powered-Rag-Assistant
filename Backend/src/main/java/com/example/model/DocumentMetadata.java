package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Record of an uploaded document.
 * Holds processing status, file details, and the list of ChromaDB vector IDs
 * so we can delete them cleanly when the document is removed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {

    private String id;
    private String originalFileName;
    private String sanitizedFileName;
    private DocumentStatus status;
    private int chunkCount;
    private long fileSize;

    /**
     * Stable IDs of every vector chunk stored in ChromaDB for this document.
     * Format: {@code {documentId}-chunk-{index}}
     */
    @Builder.Default
    private List<String> chunkIds = new ArrayList<>();

    private Instant uploadedAt;
    private String errorMessage;
}
