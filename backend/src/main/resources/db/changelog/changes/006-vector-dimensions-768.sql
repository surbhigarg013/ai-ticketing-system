--liquibase formatted sql

--changeset ticketing:006-vector-dimensions-768
-- Recreate vector table for Ollama nomic-embed-text (768 dimensions).
-- Requires re-indexing if embeddings were previously stored at 1536 (OpenAI).
DROP TABLE IF EXISTS knowledge_embeddings;

CREATE TABLE knowledge_embeddings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content     TEXT,
    metadata    JSON,
    embedding   vector(768)
);

CREATE INDEX idx_knowledge_embeddings_hnsw
    ON knowledge_embeddings
    USING hnsw (embedding vector_cosine_ops);
