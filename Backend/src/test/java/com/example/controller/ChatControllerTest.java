package com.example.controller;

import com.example.dto.ChatRequest;
import com.example.dto.ChatResponse;
import com.example.dto.SourceResponse;
import com.example.exception.GlobalExceptionHandler;
import com.example.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testChatEndpointSuccess() throws Exception {
        ChatResponse expectedResponse = ChatResponse.builder()
                .answer("Casual leave entitlement is 12 days.")
                .sources(List.of(
                        SourceResponse.builder()
                                .documentId("doc-1")
                                .documentName("leave.pdf")
                                .pageNumber(2)
                                .build()
                ))
                .build();

        when(chatService.chat(any(ChatRequest.class))).thenReturn(expectedResponse);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"What is the casual leave policy?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Casual leave entitlement is 12 days."))
                .andExpect(jsonPath("$.sources[0].documentName").value("leave.pdf"))
                .andExpect(jsonPath("$.sources[0].pageNumber").value(2));
    }
}
