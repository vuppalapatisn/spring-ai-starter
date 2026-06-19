package com.example.springai.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service that integrates Claude (via Spring AI) with MCP servers.
 *
 * How it works:
 *  1. Spring AI auto-configures McpSyncClient beans for each entry in
 *     spring.ai.mcp.client.sse.connections (or stdio.connections)
 *  2. SyncMcpToolCallbackProvider wraps all MCP tools into Spring AI ToolCallback objects
 *  3. ChatClient sends the tool schemas to Claude; Claude calls them when relevant
 *  4. Spring AI executes the actual MCP tool call and feeds results back to Claude
 */
@Service
public class McpChatService {

    private static final Logger log = LoggerFactory.getLogger(McpChatService.class);

    private final ChatClient claudeChatClient;
    private final SyncMcpToolCallbackProvider mcpToolCallbackProvider;
    private final List<McpSyncClient> mcpClients;

    public McpChatService(
            @Qualifier("claudeChatClient") ChatClient claudeChatClient,
            SyncMcpToolCallbackProvider mcpToolCallbackProvider,
            List<McpSyncClient> mcpClients) {
        this.claudeChatClient = claudeChatClient;
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
        this.mcpClients = mcpClients;
    }

    // ── 1. Single-turn chat with all MCP tools available to Claude ────────────

    public String chatWithMcpTools(String userMessage, String systemPrompt) {
        log.info("MCP chat request: {}", userMessage);

        var builder = claudeChatClient
                .prompt()
                .tools(mcpToolCallbackProvider.getToolCallbacks())
                .user(userMessage);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder = builder.system(systemPrompt);
        }

        return builder.call().content();
    }

    // ── 2. Streaming chat with MCP tools ─────────────────────────────────────

    public Flux<String> streamChatWithMcpTools(String userMessage, String systemPrompt) {
        log.info("MCP stream chat request: {}", userMessage);

        var builder = claudeChatClient
                .prompt()
                .tools(mcpToolCallbackProvider.getToolCallbacks())
                .user(userMessage);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder = builder.system(systemPrompt);
        }

        return builder.stream().content();
    }

    // ── 3. List all tools across all connected MCP servers ───────────────────

    public List<McpChatController.ToolInfo> listAvailableTools() {
        return mcpClients.stream()
                .flatMap(client -> {
                    String serverName = client.getServerInfo() != null
                            ? client.getServerInfo().name()
                            : "unknown";
                    return client.listTools().tools().stream()
                            .map(tool -> new McpChatController.ToolInfo(
                                    tool.name(),
                                    tool.description(),
                                    serverName));
                })
                .collect(Collectors.toList());
    }

    // ── 4. Execute a specific MCP tool directly ───────────────────────────────

    public Map<String, Object> executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Executing MCP tool: {} with args: {}", toolName, arguments);

        // Find the client that owns this tool
        for (McpSyncClient client : mcpClients) {
            boolean hasTool = client.listTools().tools().stream()
                    .anyMatch(t -> t.name().equals(toolName));

            if (hasTool) {
                McpSchema.CallToolResult result = client.callTool(
                        new McpSchema.CallToolRequest(toolName, arguments));

                // Extract text content from the result
                String content = result.content().stream()
                        .filter(c -> c instanceof McpSchema.TextContent)
                        .map(c -> ((McpSchema.TextContent) c).text())
                        .collect(Collectors.joining("\n"));

                return Map.of(
                        "tool", toolName,
                        "isError", result.isError() != null && result.isError(),
                        "result", content);
            }
        }

        return Map.of("error", "Tool not found: " + toolName);
    }
}
