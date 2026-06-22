# Spring AI Ecosystem Starter

Production-ready Spring Boot starter covering the full Spring AI ecosystem stack.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENTS                                │
│          Browser / curl      Mobile / SDK      Swagger UI       │
└───────────────────────┬─────────────────────────────────────────┘
                        │ HTTP / SSE
┌───────────────────────▼─────────────────────────────────────────┐
│           REST API LAYER  (Spring MVC + Spring Security)        │
│  /api/chat  /api/rag  /api/agent  /api/mcp  /api/embedding      │
│  /actuator/health  /actuator/prometheus  /swagger-ui.html       │
└───────────────────────┬─────────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────────┐
│                   SPRING AI MODULES                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌──────┐ ┌────────────┐  │
│  │  Chat   │ │   RAG   │ │  Agent  │ │ MCP  │ │ Evaluation │  │
│  │ Simple  │ │ Ingest  │ │  Tool   │ │ SSE/ │ │ Relevancy  │  │
│  │ Stream  │ │ Chunk   │ │ Calling │ │STDIO │ │ Hallucinate│  │
│  │ Memory  │ │Retrieve │ │  ReAct  │ │Tools │ │  Safety    │  │
│  └─────────┘ └─────────┘ └─────────┘ └──────┘ └────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  AiConfig — ChatClient beans wired per feature:          │   │
│  │  defaultChatClient · ragChatClient · memoryChatClient    │   │
│  │  claudeChatClient (Anthropic)                            │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────┬──────────────────────────────────┬───────────────────┘
           │                                  │
┌──────────▼──────────┐          ┌────────────▼───────────────────┐
│    LLM PROVIDERS    │          │        VECTOR STORES           │
│  OpenAI  (GPT-4o)  │          │  pgvector (default @Primary)   │
│  Anthropic (Claude) │          │  Redis · Chroma · Qdrant       │
│  Google  (Gemini)  │          └────────────────────────────────┘
│  Mistral · Ollama  │
└──────────┬──────────┘
           │
┌──────────▼──────────────────────────────────────────────────────┐
│                        PERSISTENCE                              │
│   PostgreSQL (JPA + pgvector)  Redis (chat memory)  MongoDB    │
└──────────┬──────────────────────────────────────────────────────┘
           │
┌──────────▼──────────────────────────────────────────────────────┐
│                       OBSERVABILITY                             │
│      Prometheus (:9090)   Grafana (:3000)   OpenTelemetry      │
│      AiObservability — custom metrics: latency, tokens, RAG    │
└─────────────────────────────────────────────────────────────────┘

RAG Data Flow:
  Upload Doc → Chunk + Embed → Vector Store → Similarity Search
             → LLM + Context → Answer + Sources

All infrastructure provisioned via:  docker-compose up -d
Deployment: Docker · Kubernetes (Helm + Kustomize) · GitHub Actions
```

### Module map

```
spring-ai-starter/
├── src/main/java/com/example/springai/
│   ├── SpringAiStarterApplication.java     # Entry point
│   ├── config/
│   │   ├── AiConfig.java                   # ChatClient beans (default, RAG, memory, Claude)
│   │   └── AiObservability.java            # Custom Micrometer metrics
│   ├── chat/
│   │   ├── ChatService.java                # Simple, streaming, memory, structured output
│   │   └── ChatController.java             # REST: POST /api/chat
│   ├── rag/
│   │   ├── RagService.java                 # PDF/file ingestion + RAG queries
│   │   └── RagController.java              # REST: POST /api/rag/ingest, /api/rag/query
│   ├── embedding/
│   │   └── EmbeddingService.java           # Embed text, cosine similarity
│   ├── tools/
│   │   ├── ApplicationTools.java           # @Tool-annotated functions for the LLM
│   │   └── ToolChatService.java            # ChatClient with tool access
│   ├── agent/
│   │   ├── AgentService.java               # Autonomous ReAct-style agent
│   │   └── AgentController.java            # REST: POST /api/agent/run
│   └── evaluation/
│       └── EvaluationService.java          # Relevancy, hallucination, safety, quality
├── src/test/
│   └── SpringAiStarterApplicationTests.java
├── docker-compose.yml                      # Full infra: PG+pgvector, Redis, Mongo, Chroma, Qdrant,
│                                           #             Ollama, Prometheus, Grafana, OTel
├── Dockerfile
└── pom.xml
```

## Ecosystem Coverage

| Category                  | Technologies                                        |
|---------------------------|-----------------------------------------------------|
| **Model Providers**       | OpenAI, Anthropic Claude, Google Gemini, Mistral, Ollama (Llama) |
| **Spring AI Components**  | Core, Chat, Embedding, Vector Store, Tools, Document Readers |
| **Vector Stores**         | pgvector, Redis, Chroma, Qdrant                     |
| **Memory & State**        | InMemory, Redis Chat Memory, MongoDB, PostgreSQL    |
| **Agent Frameworks**      | Spring AI native (ReAct loop via tool calling)      |
| **Knowledge Sources**     | PDF, Word/HTML/Markdown via Tika, plain text        |
| **Observability**         | Micrometer, Prometheus, Grafana, OpenTelemetry      |
| **Evaluation & Guardrails** | RelevancyEvaluator, hallucination detection, safety guardrails |
| **Tooling & Integrations** | Spring Boot, Spring Security, Spring Data, Spring WebFlux |
| **Deployment**            | Docker, Docker Compose, Kubernetes-ready            |

## Quick Start

### 1. Set API keys

```bash
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...
export MISTRAL_API_KEY=...
```

### 2. Start infrastructure

```bash
docker-compose up -d postgres redis mongo chroma qdrant
```

### 3. Run the app

```bash
./mvnw spring-boot:run
```

## API Endpoints

### Chat
```bash
# Simple chat
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Explain Spring AI in 3 sentences"}'

# Streaming (SSE)
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "Write a poem about Java"}'

# Multi-turn with memory
curl -X POST http://localhost:8080/api/chat/conversation/session-123 \
  -H "Content-Type: application/json" \
  -d '{"message": "My name is Alice"}'
```

### RAG
```bash
# Ingest a document
curl -X POST http://localhost:8080/api/rag/ingest \
  -F "file=@./docs/myfile.pdf" \
  -F "source=product-docs"

# Query
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{"question": "What are the key features?"}'
```

### Agent
```bash
curl -X POST http://localhost:8080/api/agent/run \
  -H "Content-Type: application/json" \
  -d '{"task": "What is the weather in Chennai and convert 100 USD to INR?"}'
```

## Observability

| Service        | URL                          |
|----------------|------------------------------|
| App Health     | http://localhost:8080/actuator/health |
| Prometheus     | http://localhost:9090        |
| Grafana        | http://localhost:3000        |
| RedisInsight   | http://localhost:8001        |

## Customization Points

- **Add a new tool**: Add a `@Tool` method to `ApplicationTools.java`
- **Switch vector store**: Change the active `VectorStore` bean in `AiConfig.java`
- **Add a new model**: Add the starter dependency + config keys in `application.properties`
- **Persist chat memory**: Replace `InMemoryChatMemory` with `RedisChatMemory` or `JdbcChatMemory`
- **Extend RAG pipeline**: Add custom `DocumentTransformer` steps in `RagService.ingestDocuments()`
