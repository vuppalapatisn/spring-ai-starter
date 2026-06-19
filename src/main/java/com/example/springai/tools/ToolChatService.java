package com.example.springai.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Tool-augmented chat service.
 * The LLM can autonomously decide which tools to invoke based on user intent.
 */
@Service
public class ToolChatService {

    private final ChatClient defaultChatClient;
    private final ApplicationTools applicationTools;

    public ToolChatService(ChatClient defaultChatClient, ApplicationTools applicationTools) {
        this.defaultChatClient = defaultChatClient;
        this.applicationTools = applicationTools;
    }

    /**
     * Chat with full tool access.
     * The model will call tools as needed and incorporate results automatically.
     */
    public String chatWithTools(String userMessage) {
        return defaultChatClient
                .prompt()
                .system("""
                        You are a helpful assistant with access to real-time tools.
                        Use tools when appropriate to fetch live data.
                        Always explain what you looked up and why.
                        """)
                .user(userMessage)
                .tools(applicationTools)   // registers all @Tool-annotated methods
                .call()
                .content();
    }
}
