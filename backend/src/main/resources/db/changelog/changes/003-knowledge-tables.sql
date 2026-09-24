--liquibase formatted sql

--changeset ticketing:003-knowledge-tables
CREATE TYPE knowledge_content_type AS ENUM ('DESCRIPTION', 'COMMENT', 'RESOLUTION');

CREATE TYPE indexing_trigger AS ENUM (
    'CREATE',
    'UPDATE',
    'COMMENT_ADD',
    'COMMENT_UPDATE',
    'COMMENT_DELETE',
    'STATUS_CHANGE'
);

CREATE TYPE indexing_job_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');

CREATE TABLE knowledge_document (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id           UUID NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
    content_type        knowledge_content_type NOT NULL,
    source_ref_id       UUID,
    canonical_text      TEXT NOT NULL,
    text_hash           VARCHAR(64) NOT NULL,
    ticket_status       ticket_status NOT NULL,
    ticket_priority     ticket_priority NOT NULL,
    ticket_assignee     VARCHAR(100),
    ticket_category     ticket_category NOT NULL,
    comment_created_at  TIMESTAMPTZ,
    embedding_model     VARCHAR(100),
    indexed_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_knowledge_document_unit UNIQUE (ticket_id, content_type, source_ref_id)
);

CREATE INDEX idx_knowledge_document_ticket_id ON knowledge_document (ticket_id);

CREATE TABLE indexing_job (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id       UUID NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
    trigger         indexing_trigger NOT NULL,
    status          indexing_job_status NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ
);

CREATE INDEX idx_indexing_job_status ON indexing_job (status);
CREATE INDEX idx_indexing_job_ticket_id ON indexing_job (ticket_id);
