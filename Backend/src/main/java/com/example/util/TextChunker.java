package com.example.util;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Splits extracted document text into overlapping chunks suitable for embedding.
 *
 * <p>Chunking strategy:
 * <ul>
 *   <li>Target chunk size: ~500 tokens (configurable via {@code app.rag.chunk-size})</li>
 *   <li>Minimum chunk size: 200 characters (avoids storing near-empty vectors)</li>
 *   <li>Minimum embeddable length: 10 characters</li>
 *   <li>Separators preserved: sentence boundaries (. ? ! \n)</li>
 *   <li>Maximum chunks per document: 10,000</li>
 * </ul>
 */
@Component
public class TextChunker {

    private final TokenTextSplitter splitter;

    public TextChunker() {
        this(500);
    }

    @Autowired
    public TextChunker(@Value("${app.rag.chunk-size:500}") int chunkSize) {
        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkSize)
                .withMinChunkSizeChars(Math.min(250, Math.max(50, chunkSize / 2)))
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .withPunctuationMarks(List.of('.', '?', '!', '\n'))
                .build();
    }

    /**
     * Splits a list of documents into chunks.
     *
     * @param documents source documents
     * @return chunked documents
     */
    public List<Document> chunk(List<Document> documents) {
        return splitter.apply(documents);
    }

    /**
     * Splits a single document into chunks.
     *
     * @param document source document
     * @return chunked documents
     */
    public List<Document> chunk(Document document) {
        return splitter.apply(List.of(document));
    }
}