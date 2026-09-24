package com.ticketing.rag.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VectorStoreMetadataUpdater {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public VectorStoreMetadataUpdater(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void updateMetadata(UUID knowledgeDocumentId, Map<String, Object> metadata) {
        try {
            String json = objectMapper.writeValueAsString(metadata);
            jdbcTemplate.update(
                    "UPDATE knowledge_embeddings SET metadata = ?::json WHERE id = ?::uuid", json, knowledgeDocumentId);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize vector metadata", ex);
        }
    }
}
