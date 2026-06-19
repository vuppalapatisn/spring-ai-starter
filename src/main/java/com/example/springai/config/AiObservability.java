package com.example.springai.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * AI Observability — custom metrics for monitoring AI interactions.
 *
 * Metrics exposed at /actuator/prometheus:
 *  - ai.chat.requests (counter, tagged by model)
 *  - ai.chat.latency  (timer, tagged by model)
 *  - ai.rag.queries   (counter)
 *  - ai.agent.runs    (counter, tagged by status)
 *  - ai.tokens.used   (distribution summary)
 */
@Component
public class AiObservability {

    private final MeterRegistry registry;

    public AiObservability(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Time a chat request and count it. */
    public <T> T timeChatRequest(String model, Supplier<T> action) {
        Timer timer = Timer.builder("ai.chat.latency")
                .description("Latency of AI chat requests")
                .tag("model", model)
                .register(registry);

        registry.counter("ai.chat.requests", "model", model).increment();

        long start = System.nanoTime();
        try {
            T result = action.get();
            timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            registry.counter("ai.chat.requests.success", "model", model).increment();
            return result;
        } catch (Exception e) {
            registry.counter("ai.chat.requests.error", "model", model, "error", e.getClass().getSimpleName()).increment();
            throw e;
        }
    }

    /** Record a RAG query metric. */
    public void recordRagQuery(int chunksRetrieved) {
        registry.counter("ai.rag.queries").increment();
        registry.summary("ai.rag.chunks.retrieved").record(chunksRetrieved);
    }

    /** Record an agent run. */
    public void recordAgentRun(String status) {
        registry.counter("ai.agent.runs", "status", status).increment();
    }

    /** Record token usage (for cost tracking). */
    public void recordTokenUsage(String model, int inputTokens, int outputTokens) {
        registry.summary("ai.tokens.input", "model", model).record(inputTokens);
        registry.summary("ai.tokens.output", "model", model).record(outputTokens);
        registry.summary("ai.tokens.total", "model", model).record(inputTokens + outputTokens);
    }
}
