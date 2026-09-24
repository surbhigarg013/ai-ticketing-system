--liquibase formatted sql

--changeset ticketing:005-vector-table
CREATE TABLE knowledge_embeddings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content     TEXT,
    metadata    JSON,
    embedding   vector(1536)
);

CREATE INDEX idx_knowledge_embeddings_hnsw
    ON knowledge_embeddings
    USING hnsw (embedding vector_cosine_ops);
