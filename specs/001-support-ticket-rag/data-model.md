# Data Model: AI-Powered Support Ticket Management

**Feature**: `001-support-ticket-rag` | **Date**: 2025-09-25

## Entity Relationship Overview

```text
Ticket 1──* Comment
Ticket 1──* KnowledgeDocument 1──0..1 VectorEmbedding (via PGVector store)
Ticket *──1 IndexingJob (pending jobs reference ticket)
```

Relational tables hold source of truth. Vector rows in PGVector reference `knowledge_document.id`.

---

## Ticket

Primary support request entity.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | UUID | PK, not null | Internal identifier |
| `display_id` | VARCHAR(20) | UNIQUE, not null | Human-readable, e.g. `TKT-1001` |
| `title` | VARCHAR(200) | not null, trimmed non-empty | |
| `description` | TEXT | not null | Indexed as knowledge unit |
| `status` | ENUM | not null, default `OPEN` | See state machine |
| `priority` | ENUM | not null | `LOW`, `MEDIUM`, `HIGH` |
| `assignee` | VARCHAR(100) | nullable | Display name or identifier |
| `category` | ENUM | not null, default `GENERAL` | `PAYMENT`, `SHIPMENT`, `ACCOUNT`, `GENERAL` |
| `resolution` | TEXT | nullable | Required when status ≥ RESOLVED |
| `created_at` | TIMESTAMPTZ | not null | UTC |
| `updated_at` | TIMESTAMPTZ | not null | UTC |

**Validation rules**:
- `title` must not be blank or whitespace-only.
- `priority` must be valid enum value.
- `resolution` required (non-blank) when transitioning to `RESOLVED`.

**Indexes**: `status`, `priority`, `category`, GIN/trigram on `title` + `description` for keyword search.

---

## Comment

Note attached to a ticket.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | UUID | PK | |
| `ticket_id` | UUID | FK → ticket, not null | |
| `content` | TEXT | not null, non-blank | Indexed as knowledge unit |
| `author` | VARCHAR(100) | not null | |
| `created_at` | TIMESTAMPTZ | not null | |

**Indexes**: `ticket_id`, `created_at`.

---

## TicketStatus (enum)

| Value | Terminal | Description |
|-------|----------|-------------|
| `OPEN` | No | Newly created |
| `IN_PROGRESS` | No | Actively worked |
| `RESOLVED` | No | Fix documented; awaits close |
| `CLOSED` | Yes | Completed |
| `CANCELLED` | Yes | Abandoned |

See `contracts/state-machine.md` for transition rules.

---

## Priority (enum)

`LOW` | `MEDIUM` | `HIGH`

---

## Category (enum)

`PAYMENT` | `SHIPMENT` | `ACCOUNT` | `GENERAL`

---

## KnowledgeDocument

Canonical searchable unit derived from ticket content.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | UUID | PK | Stable across re-index |
| `ticket_id` | UUID | FK, not null | |
| `content_type` | ENUM | not null | `DESCRIPTION`, `COMMENT`, `RESOLUTION` |
| `source_ref_id` | UUID | nullable | Comment id when `content_type = COMMENT` |
| `canonical_text` | TEXT | not null | Normalized searchable body |
| `text_hash` | VARCHAR(64) | not null | SHA-256 of canonical_text; skip re-embed if unchanged |
| `ticket_status` | ENUM | not null | Denormalized metadata |
| `ticket_priority` | ENUM | not null | Denormalized metadata |
| `ticket_assignee` | VARCHAR(100) | nullable | Denormalized metadata |
| `ticket_category` | ENUM | not null | Denormalized metadata |
| `comment_created_at` | TIMESTAMPTZ | nullable | For COMMENT units |
| `embedding_model` | VARCHAR(100) | nullable | Set after embed |
| `indexed_at` | TIMESTAMPTZ | nullable | Last successful embed |
| `created_at` | TIMESTAMPTZ | not null | |
| `updated_at` | TIMESTAMPTZ | not null | |

**Unique constraint**: `(ticket_id, content_type, source_ref_id)` — one row per unit.

**Canonical text format** (deterministic):
```text
Ticket: {display_id}
Status: {status} | Priority: {priority} | Category: {category}
Type: {content_type}
---
{raw_content}
```

**Re-index rules**:
| Change | Action |
|--------|--------|
| Description/comment/resolution text changes | Recompute hash; re-embed if hash differs |
| Status, priority, assignee, category only | Update metadata columns + vector store metadata; no re-embed |
| Comment deleted | Delete knowledge document + vector row |
| Ticket deleted | Cascade delete documents + vectors |

---

## IndexingJob

Reliable async re-index queue.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | UUID | PK | |
| `ticket_id` | UUID | FK, not null | |
| `trigger` | ENUM | not null | `CREATE`, `UPDATE`, `COMMENT_ADD`, `COMMENT_UPDATE`, `COMMENT_DELETE`, `STATUS_CHANGE` |
| `status` | ENUM | not null, default `PENDING` | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `attempts` | INT | not null, default 0 | |
| `last_error` | TEXT | nullable | |
| `created_at` | TIMESTAMPTZ | not null | |
| `processed_at` | TIMESTAMPTZ | nullable | |

**Processing**: Poll or event-driven; max 3 retries with exponential backoff.

---

## Vector Store (PGVector)

Managed by Spring AI `PgVectorStore`. Application stores custom metadata on each document:

| Metadata key | Type | Purpose |
|--------------|------|---------|
| `knowledgeDocumentId` | string (UUID) | Join back to relational row |
| `ticketId` | string (UUID) | Citation |
| `displayId` | string | Human-readable citation |
| `contentType` | string | `DESCRIPTION`, `COMMENT`, `RESOLUTION` |
| `status` | string | Filter at retrieval time |
| `priority` | string | Filter at retrieval time |
| `category` | string | Filter at retrieval time |

Table name: `knowledge_embeddings` (Liquibase changeset; Spring AI schema init disabled in prod, enabled in test).

---

## AssistantQuestion (transient)

Not persisted in v1.

| Field | Type | Notes |
|-------|------|-------|
| `question` | string | User natural-language query |

---

## AssistantAnswer (transient response)

| Field | Type | Notes |
|-------|------|-------|
| `answer` | string | Generated text or fixed no-match message |
| `sources` | array | `{ ticketId, contentType }` per spec |

**No-match message** (fixed constant):
```text
I could not find any relevant tickets in our history that answer this question.
```

---

## Liquibase Changelog Order

Master: `db/changelog/db.changelog-master.yaml`

| Changeset | File | Content |
|-----------|------|---------|
| `001-extensions` | `changes/001-extensions.sql` | `CREATE EXTENSION IF NOT EXISTS vector` |
| `002-ticket-tables` | `changes/002-ticket-tables.sql` | ticket, comment, sequences |
| `003-knowledge-tables` | `changes/003-knowledge-tables.sql` | knowledge_document, indexing_job |
| `004-search-indexes` | `changes/004-search-indexes.sql` | trigram/GIN for keyword search |
| `005-vector-table` | `changes/005-vector-table.sql` | PGVector table (if not delegated to Spring AI init in dev) |
