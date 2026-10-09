package com.example.repository.impl;

import com.example.model.DocumentMetadata;
import com.example.repository.DocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * File-backed persistent implementation of {@link DocumentRepository}.
 * Maintains an in-memory cache synchronized with a local JSON file.
 * Ensures document metadata survives application and server restarts.
 */
@Slf4j
@Repository
public class FileDocumentRepositoryImpl implements DocumentRepository {

    private final Map<String, DocumentMetadata> storage = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Path storagePath;
    private final Object fileLock = new Object();

    @Autowired
    public FileDocumentRepositoryImpl(
            @Autowired(required = false) ObjectMapper objectMapper,
            @Value("${app.documents.storage-path:./data/documents-registry.json}") String storagePathStr) {
        // Use the Spring-managed ObjectMapper bean (configured in AppConfig with JavaTimeModule).
        // If not available, build a fallback with auto-detected modules.
        if (objectMapper != null) {
            this.objectMapper = objectMapper;
        } else {
            ObjectMapper fallback = new ObjectMapper();
            fallback.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            fallback.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            this.objectMapper = fallback;
        }
        this.storagePath = Path.of(storagePathStr).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        loadFromDisk();
    }

    @Override
    public List<DocumentMetadata> findAll() {
        return storage.values().stream()
                .sorted((a, b) -> {
                    if (a.getUploadedAt() == null && b.getUploadedAt() == null) return 0;
                    if (a.getUploadedAt() == null) return 1;
                    if (b.getUploadedAt() == null) return -1;
                    return b.getUploadedAt().compareTo(a.getUploadedAt());
                })
                .collect(Collectors.toList());
    }

    @Override
    public Optional<DocumentMetadata> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<DocumentMetadata> findByOriginalFileNameIgnoreCase(String fileName) {
        if (fileName == null || fileName.isBlank()) return Optional.empty();
        return storage.values().stream()
                .filter(d -> d.getOriginalFileName() != null && d.getOriginalFileName().equalsIgnoreCase(fileName.trim()))
                .findFirst();
    }

    @Override
    public DocumentMetadata save(DocumentMetadata metadata) {
        if (metadata == null || metadata.getId() == null) {
            throw new IllegalArgumentException("Document metadata and ID must not be null");
        }
        storage.put(metadata.getId(), metadata);
        flushToDisk();
        return metadata;
    }

    @Override
    public void deleteById(String id) {
        if (id == null) return;
        if (storage.remove(id) != null) {
            flushToDisk();
        }
    }

    @Override
    public boolean existsById(String id) {
        return id != null && storage.containsKey(id);
    }

    @Override
    public long count() {
        return storage.size();
    }

    private void loadFromDisk() {
        synchronized (fileLock) {
            try {
                if (Files.exists(storagePath)) {
                    log.info("Loading document registry from disk: {}", storagePath);
                    Map<String, DocumentMetadata> loaded = objectMapper.readValue(
                            storagePath.toFile(),
                            new TypeReference<Map<String, DocumentMetadata>>() {}
                    );
                    if (loaded != null) {
                        storage.putAll(loaded);
                        log.info("Successfully restored {} documents from persistent registry", storage.size());
                    }
                } else {
                    log.info("No existing document registry found at {}. Starting with empty registry.", storagePath);
                    if (storagePath.getParent() != null) {
                        Files.createDirectories(storagePath.getParent());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to load document registry from disk: {}", e.getMessage(), e);
            }
        }
    }

    private void flushToDisk() {
        synchronized (fileLock) {
            try {
                if (storagePath.getParent() != null && !Files.exists(storagePath.getParent())) {
                    Files.createDirectories(storagePath.getParent());
                }

                // Write to temp file then atomic move for data integrity
                Path tempFile = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), storage);
                Files.move(tempFile, storagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                log.debug("Document registry flushed to disk: {} records saved", storage.size());
            } catch (IOException e) {
                log.error("Failed to flush document registry to disk: {}", e.getMessage(), e);
            }
        }
    }
}
