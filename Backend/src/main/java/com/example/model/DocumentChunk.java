package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single text chunk extracted from a document,
 * enriched with full metadata before being stored in ChromaDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    private String content;
    private String documentId;
    private String fileName;
    private String chunkId;
    private int chunkIndex;
    private Integer pageNumber;
    private String source;
}