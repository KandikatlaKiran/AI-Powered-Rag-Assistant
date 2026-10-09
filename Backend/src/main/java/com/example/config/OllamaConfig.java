package com.example.config;

import org.springframework.context.annotation.Configuration;

/**
 * Ollama model configuration.
 *
 * <p>The OllamaChatModel and OllamaEmbeddingModel beans are fully auto-configured
 * by Spring AI using properties defined in {@code application.properties}:
 * <ul>
 *   <li>{@code spring.ai.ollama.base-url=http://localhost:11434}</li>
 *   <li>{@code spring.ai.ollama.chat.options.model=llama3.2:3b}</li>
 *   <li>{@code spring.ai.ollama.embedding.options.model=nomic-embed-text}</li>
 * </ul>
 * No manual bean definition is required.</p>
 */
@Configuration
public class OllamaConfig {
    // Auto-configured by spring-ai-starter-model-ollama
}