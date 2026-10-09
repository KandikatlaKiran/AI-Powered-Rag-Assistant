package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

@SpringBootTest
class ChatbotUsingRagApplicationTests {

	@MockitoBean
	private VectorStore vectorStore;

	@MockitoBean
	private ChatModel chatModel;

	@MockitoBean
	private EmbeddingModel embeddingModel;

	@Test
	void contextLoads() {
	}

}
