package com.example.springai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Central AI configuration — wires together:
 *  - Multi-model ChatClient builders (OpenAI, Anthropic)
 *  - RAG-enabled ChatClient with VectorStore advisor
 *  - Memory-enabled ChatClient for conversational agents
 */
@Configuration
public class AiConfig {

    // ── Default ChatClient (OpenAI GPT-4o) ──────────────────────────────────

    @Bean
    @Primary
    public ChatClient defaultChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("You are a helpful, precise AI assistant built with Spring AI.")
                .build();
    }

    // ── RAG-enabled ChatClient ───────────────────────────────────────────────

    @Bean("ragChatClient")
    public ChatClient ragChatClient(ChatClient.Builder builder, VectorStore vectorStore) {
        return builder
                .defaultSystem("""
                        You are a knowledgeable assistant. Use the provided context documents
                        to answer questions accurately. If the answer isn't in the context,
                        say so clearly rather than guessing.
                        """)
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

    // ── Memory-enabled ChatClient ────────────────────────────────────────────

    @Bean
    public ChatMemory chatMemory() {
        // Swap for RedisChatMemory or JdbcChatMemory in production
        return new InMemoryChatMemory();
    }

    @Bean("memoryChatClient")
    public ChatClient memoryChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem("You are a conversational assistant with memory of past interactions.")
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }

    // ── Anthropic Claude ChatClient ──────────────────────────────────────────

    @Bean("claudeChatClient")
    public ChatClient claudeChatClient(
            @Qualifier("anthropicChatModel") ChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel)
                .defaultSystem("You are Claude, a thoughtful and nuanced AI assistant.")
                .build();
    }
}
