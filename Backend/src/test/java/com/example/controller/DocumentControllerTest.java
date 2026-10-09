package com.example.controller;

import com.example.dto.DocumentResponse;
import com.example.exception.GlobalExceptionHandler;
import com.example.model.DocumentStatus;
import com.example.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentController documentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(documentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetAllDocuments() throws Exception {
        DocumentResponse doc = DocumentResponse.builder()
                .id("doc-1")
                .fileName("policy.pdf")
                .status(DocumentStatus.COMPLETED)
                .chunkCount(3)
                .uploadedAt(Instant.now())
                .message("Ready")
                .build();

        when(documentService.getAllDocuments()).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("doc-1"))
                .andExpect(jsonPath("$[0].fileName").value("policy.pdf"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void testUploadDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "manual.pdf",
                "application/pdf",
                "PDF test content".getBytes()
        );

        DocumentResponse response = DocumentResponse.builder()
                .id("doc-2")
                .fileName("manual.pdf")
                .status(DocumentStatus.COMPLETED)
                .chunkCount(1)
                .uploadedAt(Instant.now())
                .message("Ready")
                .build();

        when(documentService.uploadDocument(any())).thenReturn(response);

        mockMvc.perform(multipart("/api/documents").file(file))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value("doc-2"))
                .andExpect(jsonPath("$.fileName").value("manual.pdf"));
    }

    @Test
    void testDeleteDocument() throws Exception {
        doNothing().when(documentService).deleteDocument("doc-1");

        mockMvc.perform(delete("/api/documents/doc-1"))
                .andExpect(status().isNoContent());
    }
}
