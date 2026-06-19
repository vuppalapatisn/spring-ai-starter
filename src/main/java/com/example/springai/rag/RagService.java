package com.example.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * RAG Service — Retrieval-Augmented Generation pipeline:
 *
 *  Ingestion:  File → DocumentReader → TokenTextSplitter → VectorStore
 *  Retrieval:  Query → VectorStore.similaritySearch → Context → LLM → Answer
 */
@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient ragChatClient;

    @Value("${app.rag.chunk-size:800}")
    private int chunkSize;

    @Value("${app.rag.chunk-overlap:100}")
    private int chunkOverlap;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.similarity-threshold:0.75}")
    private double similarityThreshold;

    public RagService(VectorStore vectorStore,
                      @Qualifier("ragChatClient") ChatClient ragChatClient) {
        this.vectorStore = vectorStore;
        this.ragChatClient = ragChatClient;
    }

    // ── Ingestion ────────────────────────────────────────────────────────────

    /**
     * Ingest a PDF file into the vector store.
     */
    public int ingestPdf(Resource pdfResource, String source) {
        var reader = new PagePdfDocumentReader(pdfResource);
        return ingestDocuments(reader.get(), source);
    }

    /**
     * Ingest any file type (Word, HTML, Markdown, etc.) via Apache Tika.
     */
    public int ingestFile(MultipartFile file, String source) throws IOException {
        Resource resource = file.getResource();
        var reader = new TikaDocumentReader(resource);
        return ingestDocuments(reader.get(), source);
    }

    /**
     * Ingest raw text documents directly.
     */
    public int ingestText(List<String> texts, String source) {
        List<Document> docs = texts.stream()
                .map(t -> new Document(t, Map.of("source", source)))
                .toList();
        return ingestDocuments(docs, source);
    }

    private int ingestDocuments(List<Document> documents, String source) {
        // Tag each document with source metadata
        documents.forEach(doc -> doc.getMetadata().put("source", source));

        // Chunk large documents for better retrieval granularity
        var splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
        List<Document> chunks = splitter.apply(documents);

        vectorStore.add(chunks);
        return chunks.size();
    }

    // ── Retrieval ────────────────────────────────────────────────────────────

    /**
     * Pure vector similarity search — returns raw document chunks.
     */
    public List<Document> search(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.query(query)
                        .withTopK(topK)
                        .withSimilarityThreshold(similarityThreshold)
        );
    }

    /**
     * Full RAG query — retrieves context and generates a grounded answer.
     */
    public RagResponse query(String question) {
        // Retrieve relevant chunks
        List<Document> relevantDocs = search(question);

        // Build context string
        String context = relevantDocs.stream()
                .map(Document::getContent)
                .reduce("", (a, b) -> a + "\n\n---\n\n" + b);

        // Generate answer using RAG-enabled ChatClient
        String answer = ragChatClient
                .prompt()
                .system(s -> s.text("""
                        Answer the question using only the context below.
                        If the answer is not in the context, say "I don't have enough information."
                        
                        Context:
                        {context}
                        """).param("context", context))
                .user(question)
                .call()
                .content();

        List<String> sources = relevantDocs.stream()
                .map(d -> (String) d.getMetadata().getOrDefault("source", "unknown"))
                .distinct()
                .toList();

        return new RagResponse(answer, sources, relevantDocs.size());
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record RagResponse(String answer, List<String> sources, int chunksUsed) {}
}
