package com.example.config;

import org.springframework.context.annotation.Configuration;

/**
 * ChromaDB vector store configuration.
 *
 * <p>The ChromaVectorStore bean is fully auto-configured by Spring AI using
 * properties defined in {@code application.properties}:
 * <ul>
 *   <li>{@code spring.ai.vectorstore.chroma.client.host}</li>
 *   <li>{@code spring.ai.vectorstore.chroma.client.port}</li>
 *   <li>{@code spring.ai.vectorstore.chroma.collection-name}</li>
 *   <li>{@code spring.ai.vectorstore.chroma.initialize-schema}</li>
 * </ul>
 * No manual bean definition is required.</p>
 */
@Configuration
public class ChromaConfig {
    // Auto-configured by spring-ai-starter-vector-store-chroma
}
