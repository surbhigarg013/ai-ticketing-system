package com.ticketing.rag.retrieval;

import com.ticketing.shared.exception.AiServiceUnavailableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CosineSimilarityScorer {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;

    public CosineSimilarityScorer(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
    }

    public Map<String, Double> cosineDistances(String query, List<String> documentIds) {
        if (documentIds.isEmpty()) {
            return Map.of();
        }
        try {
            float[] queryEmbedding = embeddingModel.embed(query);
            String vectorLiteral = toVectorLiteral(queryEmbedding);
            UUID[] ids = documentIds.stream().map(UUID::fromString).toArray(UUID[]::new);

            return jdbcTemplate.query(
                    """
                    SELECT id::text, (embedding <=> ?::vector) AS distance
                    FROM knowledge_embeddings
                    WHERE id = ANY(?)
                    """,
                    rs -> {
                        Map<String, Double> distances = new HashMap<>();
                        while (rs.next()) {
                            distances.put(rs.getString(1), rs.getDouble(2));
                        }
                        return distances;
                    },
                    vectorLiteral,
                    ids);
        } catch (Exception ex) {
            throw new AiServiceUnavailableException("Embedding service unavailable", ex);
        }
    }

    private static String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        return builder.append(']').toString();
    }
}
