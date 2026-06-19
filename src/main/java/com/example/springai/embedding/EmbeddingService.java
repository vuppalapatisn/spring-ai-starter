package com.example.springai.embedding;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Embedding Service — demonstrates:
 *  - Single and batch text embedding
 *  - Cosine similarity computation
 *  - Semantic similarity scoring
 */
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /** Embed a single piece of text. */
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /** Embed multiple texts in one API call. */
    public List<float[]> embedBatch(List<String> texts) {
        EmbeddingRequest request = new EmbeddingRequest(texts, null);
        EmbeddingResponse response = embeddingModel.call(request);
        return response.getResults().stream()
                .map(r -> r.getOutput())
                .toList();
    }

    /**
     * Compute cosine similarity between two texts.
     * Returns a value in [-1, 1]; higher = more similar.
     */
    public double cosineSimilarity(String text1, String text2) {
        float[] v1 = embed(text1);
        float[] v2 = embed(text2);
        return cosine(v1, v2);
    }

    /**
     * Find the most semantically similar text from a list of candidates.
     */
    public SimilarityResult findMostSimilar(String query, List<String> candidates) {
        float[] queryVec = embed(query);

        String bestMatch = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (String candidate : candidates) {
            double score = cosine(queryVec, embed(candidate));
            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }
        return new SimilarityResult(bestMatch, bestScore);
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    private double cosine(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record SimilarityResult(String text, double score) {}
}
