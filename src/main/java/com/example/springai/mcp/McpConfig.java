package com.example.springai.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Manual MCP configuration — use this when you need programmatic control
 * over MCP server connections (custom auth headers, dynamic URLs, etc.).
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  AUTO-CONFIG vs MANUAL CONFIG                                       │
 * │                                                                     │
 * │  Option A (recommended): Delete this file and configure via         │
 * │  application.properties using spring.ai.mcp.client.*               │
 * │  Spring AI will auto-create McpSyncClient + ToolCallbackProvider   │
 * │                                                                     │
 * │  Option B: Keep this file for programmatic/dynamic configuration.   │
 * │  Set spring.ai.mcp.client.enabled=false to disable auto-config.    │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@Configuration
@ConditionalOnProperty(
        name = "app.mcp.manual-config.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

    // ── SSE (HTTP) MCP Server ─────────────────────────────────────────────────

    @Bean
    @ConditionalOnProperty(name = "app.mcp.sse.url")
    public McpSyncClient sseMcpClient(@Value("${app.mcp.sse.url}") String serverUrl) {
        log.info("Connecting to SSE MCP server: {}", serverUrl);

        McpSyncClient client = McpClient.sync(
                HttpClientSseClientTransport.builder(serverUrl).build()
        ).requestTimeout(Duration.ofSeconds(30)).build();

        client.initialize();

        McpSchema.ServerCapabilities caps = client.getServerCapabilities();
        log.info("MCP server connected. Tools: {}", caps.tools() != null);

        return client;
    }

    // ── STDIO MCP Server (local process) ─────────────────────────────────────

    @Bean
    @ConditionalOnProperty(name = "app.mcp.stdio.command")
    public McpSyncClient stdioMcpClient(
            @Value("${app.mcp.stdio.command}") String command,
            @Value("${app.mcp.stdio.args:}") String[] args) {
        log.info("Launching STDIO MCP server: {} {}", command, String.join(" ", args));

        var params = new StdioClientTransport.Parameters(command, List.of(args), null);
        McpSyncClient client = McpClient.sync(
                new StdioClientTransport(params)
        ).requestTimeout(Duration.ofSeconds(30)).build();

        client.initialize();
        log.info("STDIO MCP server launched successfully");

        return client;
    }

    // ── Tool Callback Provider ────────────────────────────────────────────────

    /**
     * Wraps all McpSyncClient beans into Spring AI ToolCallback objects
     * so ChatClient can pass their schemas to Claude.
     */
    @Bean
    public SyncMcpToolCallbackProvider syncMcpToolCallbackProvider(
            List<McpSyncClient> mcpClients) {
        return new SyncMcpToolCallbackProvider(mcpClients);
    }
}
