package com.example.springai.rag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "Retrieval-Augmented Generation — ingest documents and query them with AI")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Ingest a file into the vector store",
        description = "Upload PDF, Word (.docx), HTML, or Markdown. The file is chunked and embedded automatically.",
        responses = {
            @ApiResponse(responseCode = "200", description = "File ingested successfully"),
            @ApiResponse(responseCode = "400", description = "Unsupported file type")
        }
    )
    public ResponseEntity<IngestResponse> ingest(
            @RequestParam("file") @Parameter(description = "File to ingest (PDF, Word, HTML, MD)") MultipartFile file,
            @RequestParam(value = "source", required = false) @Parameter(description = "Human-readable source label") String source)
            throws IOException {
        String sourceName = source != null ? source : file.getOriginalFilename();
        int chunks = ragService.ingestFile(file, sourceName);
        return ResponseEntity.ok(new IngestResponse(sourceName, chunks));
    }

    @PostMapping("/ingest/text")
    @Operation(
        summary = "Ingest plain text snippets",
        description = "Ingest a list of text strings directly — useful for programmatic ingestion."
    )
    public ResponseEntity<IngestResponse> ingestText(@RequestBody IngestTextRequest request) {
        int chunks = ragService.ingestText(request.texts(), request.source());
        return ResponseEntity.ok(new IngestResponse(request.source(), chunks));
    }

    @PostMapping("/query")
    @Operation(
        summary = "Ask a question using RAG",
        description = "Retrieves the most relevant document chunks from the vector store and uses them as context for the AI answer.",
        responses = @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = RagService.RagResponse.class)))
    )
    public RagService.RagResponse query(@RequestBody QueryRequest request) {
        return ragService.query(request.question());
    }

    public record IngestResponse(String source, int chunksCreated) {}
    public record IngestTextRequest(
        @Schema(description = "List of text snippets") List<String> texts,
        @Schema(description = "Source label", example = "product-docs") String source) {}
    public record QueryRequest(
        @Schema(description = "Question to ask", example = "What are the key features?") String question) {}
}
