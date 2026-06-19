package com.example.springai.agent;

import com.example.springai.tools.ApplicationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agent Service — implements a simple ReAct-style agent loop:
 *
 *  THINK → ACT (tool call) → OBSERVE → THINK → ... → ANSWER
 *
 * Spring AI handles the loop internally when tools are registered.
 * The model decides when it has enough information to stop calling tools
 * and return a final answer.
 */
@Service
public class AgentService {

    private final ChatClient defaultChatClient;
    private final ApplicationTools applicationTools;

    @Value("${app.agent.max-iterations:10}")
    private int maxIterations;

    public AgentService(ChatClient defaultChatClient, ApplicationTools applicationTools) {
        this.defaultChatClient = defaultChatClient;
        this.applicationTools = applicationTools;
    }

    /**
     * Run a general-purpose agent task.
     * The agent will autonomously plan and execute tool calls.
     */
    public AgentResult run(String task) {
        String agentId = UUID.randomUUID().toString().substring(0, 8);

        String result = defaultChatClient
                .prompt()
                .system("""
                        You are an autonomous agent that can use tools to complete tasks.
                        
                        Your approach:
                        1. Analyze the task and identify what information you need
                        2. Use available tools to gather that information
                        3. Synthesize your findings into a clear, actionable answer
                        4. Be explicit about what tools you used and what you found
                        
                        Always complete the full task before responding.
                        """)
                .user(task)
                .tools(applicationTools)
                .call()
                .content();

        return new AgentResult(agentId, task, result, "COMPLETED");
    }

    /**
     * Research agent — performs multi-step information gathering.
     */
    public AgentResult research(ResearchRequest request) {
        String prompt = """
                Research Task: %s
                
                Requirements:
                - Gather information from all relevant tools
                - Cross-reference any data you find
                - Provide a structured summary with: findings, key facts, and recommendations
                - Cite which tools provided which information
                """.formatted(request.topic());

        return run(prompt);
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record AgentResult(
            String agentId,
            String task,
            String result,
            String status
    ) {}

    public record ResearchRequest(String topic, List<String> focusAreas) {}
}
