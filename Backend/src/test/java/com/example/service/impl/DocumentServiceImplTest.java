package com.example.service.impl;

import com.example.dto.DocumentResponse;
import com.example.exception.DocumentNotFoundException;
import com.example.exception.UnsupportedDocumentTypeException;
import com.example.model.DocumentMetadata;
import com.example.model.DocumentStatus;
import com.example.repository.DocumentRepository;
import com.example.service.EmbeddingService;
import com.example.util.TextChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private TextChunker textChunker;

    @Mock
    private DocumentRepository documentRepository;

    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentServiceImpl(documentRepository, embeddingService, textChunker);
    }

    @Test
    void testGetAllDocumentsReturnsOrderedList() {
        DocumentMetadata doc1 = DocumentMetadata.builder()
                .id("1")
                .originalFileName("first.pdf")
                .sanitizedFileName("first.pdf")
                .status(DocumentStatus.COMPLETED)
                .uploadedAt(Instant.now().minusSeconds(100))
                .build();

        DocumentMetadata doc2 = DocumentMetadata.builder()
                .id("2")
                .originalFileName("second.pdf")
                .sanitizedFileName("second.pdf")
                .status(DocumentStatus.COMPLETED)
                .uploadedAt(Instant.now())
                .build();

        when(documentRepository.findAll()).thenReturn(List.of(doc2, doc1));

        List<DocumentResponse> list = documentService.getAllDocuments();
        assertEquals(2, list.size());
        assertEquals("second.pdf", list.get(0).getFileName());
    }

    @Test
    void testGetDocumentNotFoundThrowsException() {
        when(documentRepository.findById("non-existent-id")).thenReturn(Optional.empty());
        assertThrows(DocumentNotFoundException.class, () -> documentService.getDocument("non-existent-id"));
    }

    @Test
    void testDeleteDocumentRemovesVectorsAndRegistry() {
        DocumentMetadata doc = DocumentMetadata.builder()
                .id("doc-1")
                .originalFileName("test.pdf")
                .sanitizedFileName("test.pdf")
                .status(DocumentStatus.COMPLETED)
                .chunkIds(List.of("doc-1-chunk-0", "doc-1-chunk-1"))
                .uploadedAt(Instant.now())
                .build();

        when(documentRepository.findById("doc-1")).thenReturn(Optional.of(doc));

        documentService.deleteDocument("doc-1");

        verify(embeddingService).deleteByChunkIds(List.of("doc-1-chunk-0", "doc-1-chunk-1"));
        verify(documentRepository).deleteById("doc-1");
    }

    @Test
    void testUploadRejectsUnsupportedExtension() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/x-msdownload",
                "binary content".getBytes()
        );

        assertThrows(UnsupportedDocumentTypeException.class, () -> documentService.uploadDocument(exeFile));
    }

    @Test
    void testUploadRejectsEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThrows(IllegalArgumentException.class, () -> documentService.uploadDocument(emptyFile));
    }
}
