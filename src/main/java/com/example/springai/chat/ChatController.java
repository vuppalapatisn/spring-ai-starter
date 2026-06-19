package com.example.springai.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "LLM chat endpoints — simple, streaming, multi-turn, structured output")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @Operation(
        summary = "Single-turn chat",
        description = "Send a message and receive a complete AI response.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(
                value = """{"message": "Explain Spring AI in 3 sentences"}"""))),
        responses = {
            @ApiResponse(responseCode = "200", description = "AI response",
                content = @Content(schema = @Schema(implementation = ChatResponse.class))),
            @ApiResponse(responseCode = "500", description = "Model error")
        }
    )
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return new ChatResponse(chatService.chat(request.message()));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Streaming chat (SSE)",
        description = "Stream AI response tokens in real-time using Server-Sent Events.",
        responses = @ApiResponse(responseCode = "200", description = "Token stream")
    )
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        return chatService.streamChat(request.message());
    }

    @PostMapping("/conversation/{conversationId}")
    @Operation(
        summary = "Multi-turn conversation with memory",
        description = "Chat within a named session. The AI remembers previous messages in this session.",
        parameters = @Parameter(name = "conversationId", description = "Unique session ID, e.g. 'user-42-session-1'", required = true)
    )
    public ChatResponse conversationalChat(
            @RequestBody ChatRequest request,
            @PathVariable String conversationId) {
        return new ChatResponse(chatService.conversationalChat(request.message(), conversationId));
    }

    @PostMapping("/explain")
    @Operation(
        summary = "Explain a topic to an audience",
        description = "Uses a prompt template to tailor the explanation to the target audience."
    )
    public ChatResponse explain(
            @RequestParam @Parameter(description = "Topic to explain, e.g. 'Spring AI'") String topic,
            @RequestParam(defaultValue = "beginners") @Parameter(description = "Target audience") String audience) {
        return new ChatResponse(chatService.chatWithTemplate(topic, audience));
    }

    @PostMapping("/extract")
    @Operation(
        summary = "Structured output extraction",
        description = "Extract structured product data from unstructured text using AI.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(
                value = """{"message": "The iPhone 16 Pro costs $999, it's a smartphone with 48MP camera and A18 chip"}""")))
    )
    public ChatService.ProductInfo extractProduct(@RequestBody ChatRequest request) {
        return chatService.extractProductInfo(request.message());
    }

    public record ChatRequest(
        @Schema(description = "User message", example = "What is Spring AI?") String message) {}

    public record ChatResponse(
        @Schema(description = "AI-generated response") String response) {}
}
