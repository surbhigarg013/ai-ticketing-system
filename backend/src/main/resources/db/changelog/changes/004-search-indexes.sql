--liquibase formatted sql

--changeset ticketing:004-search-indexes
CREATE INDEX idx_ticket_status ON ticket (status);
CREATE INDEX idx_ticket_priority ON ticket (priority);
CREATE INDEX idx_ticket_category ON ticket (category);
CREATE INDEX idx_ticket_created_at ON ticket (created_at DESC);

CREATE INDEX idx_ticket_title_trgm ON ticket USING GIN (title gin_trgm_ops);
CREATE INDEX idx_ticket_description_trgm ON ticket USING GIN (description gin_trgm_ops);
