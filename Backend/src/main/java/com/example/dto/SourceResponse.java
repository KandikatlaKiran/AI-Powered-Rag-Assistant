package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a source document chunk that contributed to a RAG answer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceResponse {

    private String documentId;
    private String documentName;
    private Integer pageNumber;
    private Integer chunkIndex;
    private String snippet;
}
