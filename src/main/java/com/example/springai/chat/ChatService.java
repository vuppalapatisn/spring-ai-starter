package com.example.springai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Chat Service — covers three usage patterns:
 *  1. Simple single-turn chat
 *  2. Streaming response (Server-Sent Events)
 *  3. Structured output (entity extraction)
 */
@Service
public class ChatService {

    private final ChatClient defaultChatClient;
    private final ChatClient memoryChatClient;

    public ChatService(
            ChatClient defaultChatClient,
            @Qualifier("memoryChatClient") ChatClient memoryChatClient) {
        this.defaultChatClient = defaultChatClient;
        this.memoryChatClient = memoryChatClient;
    }

    // ── 1. Simple chat ───────────────────────────────────────────────────────

    public String chat(String userMessage) {
        return defaultChatClient
                .prompt()
                .user(userMessage)
                .call()
                .content();
    }

    // ── 2. Template-based chat ───────────────────────────────────────────────

    public String chatWithTemplate(String topic, String audience) {
        PromptTemplate template = new PromptTemplate("""
                Explain {topic} in simple terms suitable for {audience}.
                Be concise, practical, and use real examples.
                """);
        Prompt prompt = template.create(Map.of("topic", topic, "audience", audience));
        return defaultChatClient
                .prompt(prompt)
                .call()
                .content();
    }

    // ── 3. Streaming chat (returns Flux for SSE) ─────────────────────────────

    public Flux<String> streamChat(String userMessage) {
        return defaultChatClient
                .prompt()
                .user(userMessage)
                .stream()
                .content();
    }

    // ── 4. Multi-turn chat with memory (per conversationId) ──────────────────

    public String conversationalChat(String userMessage, String conversationId) {
        return memoryChatClient
                .prompt()
                .user(userMessage)
                .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                .call()
                .content();
    }

    // ── 5. Structured output extraction ─────────────────────────────────────

    public ProductInfo extractProductInfo(String description) {
        return defaultChatClient
                .prompt()
                .user(u -> u.text("""
                        Extract product information from the following description.
                        Return a JSON object with: name, price, category, features (list).
                        
                        Description: {description}
                        """)
                        .param("description", description))
                .call()
                .entity(ProductInfo.class);
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record ProductInfo(
            String name,
            Double price,
            String category,
            java.util.List<String> features
    ) {}
}
