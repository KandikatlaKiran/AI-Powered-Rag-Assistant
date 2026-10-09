package com.example.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    private TextChunker textChunker;

    @BeforeEach
    void setUp() {
        textChunker = new TextChunker();
    }

    @Test
    void testChunkSingleDocument() {
        String content = "Spring AI provides high-level abstractions for developing AI applications. "
                + "It integrates seamlessly with Spring Boot and supports multiple model providers including Ollama. "
                + "Vector stores like ChromaDB are supported for RAG architectures.";

        Document doc = new Document(content);
        List<Document> chunks = textChunker.chunk(doc);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).getText().contains("Spring AI"));
    }

    @Test
    void testChunkListDocuments() {
        Document doc1 = new Document("Document 1 content about company policy and procedures.");
        Document doc2 = new Document("Document 2 content about employee benefits and health coverage.");

        List<Document> chunks = textChunker.chunk(List.of(doc1, doc2));

        assertNotNull(chunks);
        assertTrue(chunks.size() >= 2);
    }
}
