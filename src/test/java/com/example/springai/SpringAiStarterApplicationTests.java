package com.example.springai;

import com.example.springai.chat.ChatService;
import com.example.springai.rag.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring AI services.
 *
 * Uses spring-ai-test fixtures which provide mock chat/embedding models
 * so tests run without real API keys.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-key",
    "spring.ai.openai.chat.options.model=gpt-4o-mini"
})
class SpringAiStarterApplicationTests {

    @Autowired
    private ChatService chatService;

    @Autowired
    private RagService ragService;

    @Test
    void contextLoads() {
        // Verifies the entire Spring context wires up correctly
    }

    @Test
    void chatServiceReturnsNonEmptyResponse() {
        String response = chatService.chat("What is Spring AI?");
        assertThat(response).isNotBlank();
    }

    @Test
    void ragServiceIngestsAndQueriesDocuments() {
        // Ingest test documents
        List<String> docs = List.of(
            "Spring AI is a framework for building AI-powered applications in Java.",
            "It provides abstractions for chat, embeddings, vector stores, and tool calling.",
            "Spring AI supports multiple model providers including OpenAI, Anthropic, and Ollama."
        );
        int chunks = ragService.ingestText(docs, "test-docs");
        assertThat(chunks).isGreaterThan(0);

        // Query the ingested documents
        RagService.RagResponse response = ragService.query("What is Spring AI?");
        assertThat(response.answer()).isNotBlank();
        assertThat(response.sources()).contains("test-docs");
    }

    @Test
    void structuredOutputExtractionWorks() {
        String description = "The iPhone 16 Pro costs $999 and is a premium smartphone with " +
                             "titanium build, 48MP camera, and A18 Pro chip.";
        ChatService.ProductInfo info = chatService.extractProductInfo(description);
        assertThat(info).isNotNull();
        assertThat(info.name()).isNotBlank();
    }
}
