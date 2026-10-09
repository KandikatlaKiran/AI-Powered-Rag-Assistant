package com.example.controller;

import com.example.dto.ChatRequest;
import com.example.dto.ChatResponse;
import com.example.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the RAG chat endpoint.
 *
 * <pre>
 * POST /api/chat — Submit a question and receive a grounded answer with sources (synchronous)
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Processes a question through the full RAG pipeline.
     *
     * @param request validated chat request containing the user's question
     * @return 200 OK with answer and source document references
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("POST /api/chat — question length={}", request.getQuestion().length());
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }
}
