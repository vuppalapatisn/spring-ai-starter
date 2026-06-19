package com.example.springai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI configuration.
 *
 * Swagger UI:  http://localhost:8080/swagger-ui.html
 * API Docs:    http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:spring-ai-starter}")
    private String appName;

    @Bean
    public OpenAPI springAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring AI Ecosystem API")
                        .description("""
                                Production-ready Spring AI REST API covering:
                                - **Chat** — simple, streaming, multi-turn with memory, structured output
                                - **RAG** — document ingestion (PDF, Word, text) + retrieval-augmented generation
                                - **Agents** — autonomous tool-calling agents
                                - **Embeddings** — text vectorisation and semantic similarity
                                - **Evaluation** — hallucination detection, safety guardrails, quality scoring
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Spring AI Team")
                                .url("https://spring.io/projects/spring-ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Spring AI Documentation")
                        .url("https://docs.spring.io/spring-ai/reference/"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.staging.example.com").description("Staging"),
                        new Server().url("https://api.example.com").description("Production")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token from /api/auth/login")));
    }
}
