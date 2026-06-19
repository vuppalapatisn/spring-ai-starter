package com.example.springai.mcp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing Claude + MCP server integration endpoints.
 *
 * Endpoints:
 *   POST /api/mcp/chat              — single-turn chat with MCP tools
 *   POST /api/mcp/chat/stream       — streaming chat with MCP tools
 *   GET  /api/mcp/tools             — list all tools from connected MCP servers
 *   POST /api/mcp/execute-tool      — call one specific MCP tool directly
 */
@RestController
@RequestMapping("/api/mcp")
@Tag(name = "MCP Chat", description = "Claude + MCP Server integration")
public class McpChatController {

    private final McpChatService mcpChatService;

    public McpChatController(McpChatService mcpChatService) {
        this.mcpChatService = mcpChatService;
    }

    // ── 1. Chat with MCP tools ────────────────────────────────────────────────

    @PostMapping("/chat")
    @Operation(summary = "Chat with Claude using MCP tools",
               description = "Claude automatically calls connected MCP server tools when needed")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String response = mcpChatService.chatWithMcpTools(request.message(), request.systemPrompt());
        return new ChatResponse(response);
    }

    // ── 2. Streaming chat with MCP tools ─────────────────────────────────────

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream chat with Claude using MCP tools (SSE)")
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        return mcpChatService.streamChatWithMcpTools(request.message(), request.systemPrompt());
    }

    // ── 3. List available MCP tools ───────────────────────────────────────────

    @GetMapping("/tools")
    @Operation(summary = "List all tools available from connected MCP servers")
    public List<ToolInfo> listTools() {
        return mcpChatService.listAvailableTools();
    }

    // ── 4. Execute a specific MCP tool directly ───────────────────────────────

    @PostMapping("/execute-tool")
    @Operation(summary = "Execute a specific MCP tool directly (without Claude)",
               description = "Useful for testing individual MCP tools")
    public Map<String, Object> executeTool(
            @Parameter(description = "Tool name as reported by the MCP server")
            @RequestParam String toolName,
            @RequestBody Map<String, Object> arguments) {
        return mcpChatService.executeTool(toolName, arguments);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record ChatRequest(
            String message,
            String systemPrompt   // optional — pass null to use the default
    ) {}

    public record ChatResponse(String response) {}

    public record ToolInfo(String name, String description, String serverName) {}
}
