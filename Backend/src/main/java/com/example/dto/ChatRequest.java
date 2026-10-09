package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming request payload for the chat/RAG endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Question must not be blank")
    @Size(min = 2, max = 2000, message = "Question must be between 2 and 2000 characters")
    private String question;
}