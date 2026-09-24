# Research: AI-Powered Support Ticket Management

**Feature**: `001-support-ticket-rag` | **Date**: 2025-09-25

## 1. Spring Boot + Spring AI Version Pair

**Decision**: Spring Boot **3.5.x** + Spring AI **1.1.x** (BOM-managed).

**Rationale**:
- Spring AI 1.1.x branch targets Spring Boot 3.5.x per official compatibility matrix.
- Both are stable GA on Maven Central; no snapshot repos required.
- Java 21 is first-class on this pair.
- `spring-ai-starter-vector-store-pgvector` ships PGVector auto-configuration.

**Alternatives considered**:
| Pair | Rejected because |
|------|------------------|
| Spring Boot 3.4.x + Spring AI 1.0.x | Older; 3.5 + 1.1 is current stable compatible line |
| Spring Boot 4.x + Spring AI 2.0.x | Newer but higher churn; user asked stable compatible pair for greenfield v1 |

## 2. Schema Migrations: Liquibase

**Decision**: **Liquibase** for this feature.

**Rationale**:
- Constitution §I mandates Liquibase for all schema changes.
- Spring Boot auto-configures via `spring.liquibase.change-log`.
- SQL changesets in `db/changelog/changes/` handle PGVector extension DDL (`CREATE EXTENSION vector`).
- Changelog supports ordered includes and rollback metadata for complex migrations.

**Alternatives considered**:
| Option | Rejected because |
|--------|------------------|
| Flyway | Contradicts constitution; no project precedent for dual migration tools |
| Spring AI `initialize-schema: true` only | No versioned relational DDL for tickets/comments; unsuitable for prod |

## 3. Package Layout: Package-by-Feature

**Decision**: Two top-level feature packages — `ticket` and `rag` — plus `shared` for cross-cutting config and exceptions.

**Rationale**:
- User requires deterministic ticket management separate from AI/RAG.
- Each feature owns api/domain/service/repository (ticket) or api/ingestion/retrieval/service (rag).
- Prevents RAG concerns leaking into ticket state machine code.

**Alternatives considered**:
| Layout | Rejected because |
|--------|------------------|
| Layer-only (`controller/service/repository`) | Cross-feature coupling; hard to enforce RAG isolation |
| Multi-module Maven (`ticket-module`, `rag-module`) | Over-engineering for v1 single deployable |

## 4. Knowledge Document Granularity

**Decision**: One canonical knowledge document per ticket content unit — description, each comment, resolution field (when present). No further sub-chunking for v1.

**Rationale**:
- Matches spec clarifications (separate indexed units per section).
- Each unit maps 1:1 to a citation `contentType` + `ticketId`.
- Simplifies re-index: diff units by `(ticketId, contentType, contentId)`.

**Alternatives considered**:
| Strategy | Rejected because |
|----------|------------------|
| Whole-ticket single document | Loses comment-level citation precision |
| Fixed-token sliding window chunks | Over-chunking; citation granularity unclear for short tickets |

## 5. Embedding Model

**Decision**: Profile-based provider selection via Spring AI:

| Profile | Provider | Model | Dimensions |
|---------|----------|-------|------------|
| `local` (default) | Ollama | `nomic-embed-text` | **768** |
| `openai` | OpenAI | `text-embedding-3-small` | **1536** |

Configured in `application-local.yml` / `application-openai.yml`. Vector table dimension must match the active profile (`EMBEDDING_DIMENSIONS` env var; Liquibase changeset `006-vector-dimensions-768.sql` for local).

**Rationale**:
- Local dev runs without cloud API keys; Ollama has first-class Spring AI support.
- Production can switch to OpenAI via `SPRING_PROFILES_ACTIVE=openai` without code changes.
- Model ids externalized per profile (`spring.ai.ollama.embedding.options.model` / `spring.ai.openai.embedding.options.model`).

**Alternatives considered**:
| Model | Rejected because |
|-------|------------------|
| OpenAI-only | Requires API key for local development |
| Local ONNX embeddings | Extra JVM memory; Ollama covers both embed + chat in one daemon |
| `text-embedding-3-large` | Higher cost; marginal gain on short ticket text |

## 6. Vector Store & Similarity

**Decision**: Spring AI `PgVectorStore` with cosine distance, HNSW index. Similarity threshold applied post-retrieval on cosine similarity score (0–1, higher = more similar).

**Rationale**:
- Official Spring AI PGVector starter; Testcontainers `pgvector/pgvector` image available.
- Cosine distance is standard for text embeddings.
- Threshold + top-k externalized in `@ConfigurationProperties` prefix `app.rag.retrieval`.

**Alternatives considered**:
| Approach | Rejected because |
|----------|------------------|
| Raw SQL `<->` operator bypassing Spring AI | Loses auto-config; more custom code |
| Inner-product distance | Less intuitive threshold tuning for operators |

**Defaults** (documented, overridable):
- `top-k`: 5
- `similarity-threshold`: **0.70** (`local` profile) / **0.75** (`openai` profile)

## 7. Re-Index Strategy

**Decision**: Transactional outbox pattern within same DB transaction as ticket mutation.

Flow:
1. Ticket service commits ticket/comment change.
2. Same transaction inserts `indexing_job` row (PENDING).
3. `@TransactionalEventListener(phase = AFTER_COMMIT)` or scheduled poller processes job.
4. Job rebuilds affected knowledge documents, re-embeds changed text, upserts vectors, updates metadata-only rows without re-embed.
5. Job marks COMPLETED or FAILED with retry count.

**Rationale**:
- Reliable re-index after commit; no lost events on rollback.
- Idempotent upsert by `knowledge_document_id`.
- Metadata-only updates skip embedding API call.

**Alternatives considered**:
| Approach | Rejected because |
|----------|------------------|
| Synchronous embed in request thread | Slow writes; LLM API failures block ticket updates |
| `@Async` without outbox | Lost jobs on crash between commit and async dispatch |

## 8. Grounding Guard

**Decision**: `GroundingGuard` service between retrieval and LLM call.

Rules:
- If zero chunks above threshold → return fixed no-match response; **no LLM call**.
- If chunks present → build prompt with retrieved text only; system prompt forbids external knowledge.
- Post-generation validation: response must reference only retrieved `ticketId` values (optional lightweight check).

**Rationale**: Spec FR-020, FR-022; constitution RAG Integrity principle.

## 9. LLM for Answer Generation

**Decision**: Profile-based chat model via Spring AI `ChatClient`:

| Profile | Provider | Model |
|---------|----------|-------|
| `local` (default) | Ollama | `llama3.2:3b` |
| `openai` | OpenAI | `gpt-4o-mini` |

OpenAI auto-configuration is excluded on the `local` profile (and vice versa) to prevent startup failures when only one provider is configured.

**Rationale**:
- Local dev: lightweight 3B model runs on consumer hardware (~2 GB).
- Production: `gpt-4o-mini` is cost-effective for short grounded Q&A.
- Spring AI `ChatClient` supports structured prompt templates for both providers.

## 10. Integration Testing

**Decision**: Testcontainers with `pgvector/pgvector:pg16` for integration tests covering state machine and RAG retrieval.

**Rationale**:
- Real PostgreSQL + vector extension behavior.
- `@SpringBootTest` + Testcontainers for end-to-end ticket → index → query flows.
- `@WebMvcTest` for controller contracts; unit tests for `TicketStateMachine` and `GroundingGuard`.

**Alternatives considered**:
| Approach | Rejected because |
|----------|------------------|
| H2 with mocked vector store | No real similarity search behavior |
| Embedded PG without vector | Cannot validate retrieval thresholds |

## 11. Frontend Stack

**Decision**: React 19 + TypeScript + Vite; Vitest + React Testing Library.

**Rationale**: Constitution requires React + TypeScript; Vite is standard for greenfield SPAs.

## 12. API Contract Format

**Decision**: OpenAPI 3.1 YAML in `contracts/`; springdoc-openapi generates runtime docs from annotations kept in sync.

**Rationale**: Project rules require OpenAPI as contract source.

## 13. Ticket Identifier Format

**Decision**: Human-readable `TKT-{sequence}` generated via DB sequence or `ticket_number_seq`.

**Rationale**: Spec examples use TKT-1001; opaque UUID as internal PK, display id separate.
