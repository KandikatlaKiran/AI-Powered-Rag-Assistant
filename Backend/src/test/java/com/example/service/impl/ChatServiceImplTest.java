package com.example.service.impl;

import com.example.dto.ChatRequest;
import com.example.dto.ChatResponse;
import com.example.model.DocumentMetadata;
import com.example.model.DocumentStatus;
import com.example.repository.DocumentRepository;
import com.example.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "topK", 4);
        ReflectionTestUtils.setField(chatService, "maxContextCharacters", 4000);
    }

    @Test
    void testChatDocumentListingIntentReturnsDocumentInventory() {
        DocumentMetadata doc1 = DocumentMetadata.builder()
                .id("doc-1")
                .originalFileName("handbook.pdf")
                .status(DocumentStatus.COMPLETED)
                .chunkCount(4)
                .uploadedAt(Instant.now())
                .build();

        when(documentRepository.findAll()).thenReturn(List.of(doc1));

        ChatRequest request = new ChatRequest("Which documents do you have?");
        ChatResponse response = chatService.chat(request);

        assertNotNull(response);
        assertTrue(response.getAnswer().contains("Available documents:"));
        assertTrue(response.getAnswer().contains("handbook.pdf"));
        verifyNoInteractions(embeddingService);
        verifyNoInteractions(chatModel);
    }

    @Test
    void testChatWhenNoDocumentsExistReturnsEmptyMessage() {
        when(documentRepository.findAll()).thenReturn(Collections.emptyList());

        ChatRequest request = new ChatRequest("What is the leave policy?");
        ChatResponse response = chatService.chat(request);

        assertNotNull(response);
        assertTrue(response.getAnswer().contains("No documents are currently available"));
        assertTrue(response.getSources().isEmpty());
        verifyNoInteractions(embeddingService);
        verifyNoInteractions(chatModel);
    }

    @Test
    void testChatWhenNoChunksFoundReturnsFallback() {
        DocumentMetadata doc1 = DocumentMetadata.builder()
                .id("doc-1")
                .originalFileName("handbook.pdf")
                .status(DocumentStatus.COMPLETED)
                .chunkCount(4)
                .uploadedAt(Instant.now())
                .build();

        when(documentRepository.findAll()).thenReturn(List.of(doc1));
        when(embeddingService.searchSimilar(anyString(), anyInt())).thenReturn(Collections.emptyList());

        ChatRequest request = new ChatRequest("What is the leave policy?");
        ChatResponse response = chatService.chat(request);

        assertNotNull(response);
        assertTrue(response.getAnswer().contains("not available in the uploaded documents"));
        assertTrue(response.getSources().isEmpty());
        verifyNoInteractions(chatModel);
    }

    @Test
    void testChatWithRelevantChunksCallsLLMAndExtractsSources() {
        DocumentMetadata doc1 = DocumentMetadata.builder()
                .id("doc-123")
                .originalFileName("handbook.pdf")
                .status(DocumentStatus.COMPLETED)
                .chunkCount(4)
                .uploadedAt(Instant.now())
                .build();

        when(documentRepository.findAll()).thenReturn(List.of(doc1));

        Document chunk1 = new Document(
                "chunk-1",
                "Employees get 15 days paid leave.",
                Map.of(
                        "documentId", "doc-123",
                        "fileName", "handbook.pdf",
                        "chunkId", "doc-123-chunk-0",
                        "chunkIndex", 0,
                        "pageNumber", 3
                )
        );

        when(embeddingService.searchSimilar(eq("How much leave?"), eq(4))).thenReturn(List.of(chunk1));

        org.springframework.ai.chat.messages.AssistantMessage assistantMessage =
                new org.springframework.ai.chat.messages.AssistantMessage("Employees receive 15 days paid annual leave.");
        Generation generation = new Generation(assistantMessage);
        org.springframework.ai.chat.model.ChatResponse aiResponse =
                new org.springframework.ai.chat.model.ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(aiResponse);

        ChatRequest request = new ChatRequest("How much leave?");
        ChatResponse response = chatService.chat(request);

        assertNotNull(response);
        assertEquals("Employees receive 15 days paid annual leave.", response.getAnswer());
        assertEquals(1, response.getSources().size());
        assertEquals("handbook.pdf", response.getSources().get(0).getDocumentName());
        assertEquals(3, response.getSources().get(0).getPageNumber());
    }
}
