package com.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Application-level configuration.
 *
 * <p>Document metadata persistence is handled by {@link com.example.repository.DocumentRepository}.</p>
 *
 * <p>RAG tuning parameters ({@code topK}) are bound here and injected into
 * services via {@code @Value}, keeping them out of application logic.</p>
 */
@Configuration
public class AppConfig {

    @Value("${app.rag.similarity-threshold:0.35}")
    private double similarityThreshold;

    @Value("${app.rag.max-candidates:15}")
    private int maxCandidates;

    @Value("${app.rag.max-context-characters:4000}")
    private int maxContextCharacters;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register JavaTimeModule so java.time.Instant serializes as ISO-8601 strings
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public int getMaxContextCharacters() {
        return maxContextCharacters;
    }
}
