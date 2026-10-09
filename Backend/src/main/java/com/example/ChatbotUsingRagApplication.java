package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * RAG Assistant Application.
 *
 * <p>Full-stack RAG pipeline using:
 * <ul>
 *   <li>Spring AI + Ollama (Llama 3.2 for generation, nomic-embed-text for embeddings)</li>
 *   <li>ChromaDB (Docker) as the vector store</li>
 *   <li>Apache Tika for multi-format text extraction (PDF, DOCX, TXT)</li>
 *   <li>Spring AI TokenTextSplitter for semantic chunking</li>
 * </ul>
 */
@EnableAsync
@SpringBootApplication
public class ChatbotUsingRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotUsingRagApplication.class, args);
    }
}
