--liquibase formatted sql

--changeset ticketing:002-ticket-tables
CREATE TYPE ticket_status AS ENUM (
    'OPEN',
    'IN_PROGRESS',
    'RESOLVED',
    'CLOSED',
    'CANCELLED'
);

CREATE TYPE ticket_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH');

CREATE TYPE ticket_category AS ENUM ('PAYMENT', 'SHIPMENT', 'ACCOUNT', 'GENERAL');

CREATE SEQUENCE ticket_display_id_seq START WITH 1001 INCREMENT BY 1;

CREATE TABLE ticket (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_id      VARCHAR(20) NOT NULL UNIQUE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    status          ticket_status NOT NULL DEFAULT 'OPEN',
    priority        ticket_priority NOT NULL,
    assignee        VARCHAR(100),
    category        ticket_category NOT NULL DEFAULT 'GENERAL',
    resolution      TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE comment (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id   UUID NOT NULL REFERENCES ticket (id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    author      VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comment_ticket_id ON comment (ticket_id);
CREATE INDEX idx_comment_created_at ON comment (created_at);
