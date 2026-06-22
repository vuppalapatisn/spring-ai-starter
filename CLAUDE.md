# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean package

# Run (requires API keys + infrastructure — see below)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=SpringAiStarterApplicationTests

# Start only the infrastructure services needed for local dev
docker-compose up -d postgres redis mongo chroma qdrant

# Start full observability stack too
docker-compose up -d
```

## Required environment variables

```bash
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...
export MISTRAL_API_KEY=...
# Optional: only if using MCP integrations
export BRAVE_API_KEY=...
export GITHUB_TOKEN=...
```

DB credentials default to `springai/springai` (overridable via `DB_USERNAME` / `DB_PASSWORD`).

## Architecture

The app is split into feature packages under `com.example.springai`, each with a `*Service` (business logic) and `*Controller` (REST layer):

- **`config/`** — Central wiring point. `AiConfig` defines four named `ChatClient` beans: `defaultChatClient` (OpenAI, `@Primary`), `ragChatClient` (with `QuestionAnswerAdvisor`), `memoryChatClient` (with `MessageChatMemoryAdvisor`), and `claudeChatClient` (Anthropic). `AiObservability` registers custom Micrometer metrics for token tracking and latency. `OpenApiConfig` sets up Swagger grouping.

- **`chat/`** — Five patterns in `ChatService`: single-turn, prompt-template, SSE streaming (`Flux`), multi-turn via `memoryChatClient` keyed on `conversationId`, and structured output extraction to Java records.

- **`rag/`** — `RagService` handles document ingestion (PDF via `PagePdfDocumentReader`, other formats via Apache Tika) with configurable chunking (`app.rag.*` properties), stores to pgvector, then answers queries via `ragChatClient`. Returns answer + source metadata.

- **`embedding/`** — Direct access to `EmbeddingModel` for single/batch embeddings and cosine-similarity ranking. Does not go through `ChatClient`.

- **`tools/`** — `ApplicationTools` contains `@Tool`-annotated methods (weather, currency, product lookup, datetime). `ToolChatService` builds a `ChatClient` with `.defaultTools(applicationTools)` so the model can invoke them autonomously.

- **`agent/`** — `AgentService` uses the same tool-calling `ChatClient` as above but structures tasks as a ReAct loop. Spring AI handles the tool-call/response iteration internally.

- **`mcp/`** — `McpChatService` wires Claude (`claudeChatClient`) to `SyncMcpToolCallbackProvider`, which auto-discovers all configured MCP servers. MCP connections are defined in `application.properties` under `spring.ai.mcp.client.sse.connections.*` or `spring.ai.mcp.client.stdio.connections.*`. `McpConfig` offers a programmatic alternative when `app.mcp.manual-config.enabled=true`.

- **`evaluation/`** — `EvaluationService` uses Spring AI's `RelevancyEvaluator` and additional LLM prompts for hallucination detection, safety guardrails, and quality scoring. No REST controller — intended to be called from tests or other services.

## Key design decisions

- The active `VectorStore` bean injected everywhere defaults to pgvector. To switch stores (Redis, Chroma, Qdrant), change which `VectorStore` bean is marked `@Primary` in `AiConfig` or use `@Qualifier`.
- `InMemoryChatMemory` is used in dev. For production, swap for `RedisChatMemory` or `JdbcChatMemory` in the `chatMemory()` bean.
- All model provider starters are included in `pom.xml` but only the providers with valid API keys will work at runtime. Spring AI auto-configures each model if its `api-key` property resolves.
- RAG chunk size (800 tokens, 100 overlap) and retrieval settings (top-k=5, threshold=0.75) are tunable via `app.rag.*` properties without code changes.
- MCP is disabled by default (no connections configured). Enable by uncommenting server blocks in `application.properties`. Do not set both `spring.ai.mcp.client.enabled=true` and `app.mcp.manual-config.enabled=true` simultaneously — this creates duplicate beans.

## Observability endpoints (local)

| Endpoint | URL |
|---|---|
| Health | http://localhost:8080/actuator/health |
| Prometheus scrape | http://localhost:8080/actuator/prometheus |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Grafana | http://localhost:3000 (admin/admin) |
| Prometheus | http://localhost:9090 |
