package com.example.springai.agent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent", description = "Autonomous AI agents — multi-step reasoning with tool calling")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/run")
    @Operation(
        summary = "Run an autonomous agent task",
        description = "The agent plans, calls tools as needed, and synthesises a final answer.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(
                value = """{"task": "What is the weather in Chennai and convert 1000 INR to USD?"}""")))
    )
    public AgentService.AgentResult runTask(@RequestBody TaskRequest request) {
        return agentService.run(request.task());
    }

    @PostMapping("/research")
    @Operation(
        summary = "Run a research agent",
        description = "Multi-step information gathering across tools, returning a structured report.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(examples = @ExampleObject(
                value = """{"topic": "Spring AI ecosystem", "focusAreas": ["vector stores", "agents"]}""")))
    )
    public AgentService.AgentResult research(@RequestBody AgentService.ResearchRequest request) {
        return agentService.research(request);
    }

    public record TaskRequest(String task) {}
}
