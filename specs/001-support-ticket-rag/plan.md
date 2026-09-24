# Implementation Plan: AI-Powered Support Ticket Management

**Branch**: `001-support-ticket-rag` | **Date**: 2025-09-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-support-ticket-rag/spec.md`

## Summary

Build a support ticket management system with server-enforced lifecycle state machine and RAG-based Q&A over ticket history. Backend: Java 21, Spring Boot 3.5.x, Spring AI 1.1.x, PostgreSQL + PGVector, Liquibase, Spring Data JPA. Frontend: React + TypeScript. Package-by-feature layout keeps deterministic ticket domain separate from RAG pipeline. RAG flow: ticket content → canonical knowledge documents → embeddings → PGVector → similarity retrieval → grounding guard → LLM → answer + structured sources.

## Technical Context

**Language/Version**: Java 21 (backend), TypeScript 5.x (frontend)

**Primary Dependencies**:
- Spring Boot 3.5.x, Spring AI 1.1.x BOM
- `spring-ai-starter-vector-store-pgvector`
- `spring-ai-starter-model-openai` (embedding + chat)
- Spring Data JPA, Bean Validation, MapStruct, Lombok
- Liquibase, springdoc-openapi
- React 19, Vite, Vitest, React Testing Library

**Storage**: PostgreSQL 16 + PGVector extension; relational tables + vector store table

**Testing**: JUnit 5, Mockito, AssertJ, `@SpringBootTest`, Testcontainers (`pgvector/pgvector:pg16`), `@WebMvcTest`, Vitest (frontend)

**Target Platform**: Linux/macOS server + browser SPA

**Performance Goals** (supplementary NFRs — traceable to spec success criteria):

| Target | Plan NFR | Spec linkage |
|--------|----------|--------------|
| Ticket CRUD p95 < 200ms | Performance Goals | SC-001, SC-008 (agent workflow time) |
| Assistant Q&A p95 < 5s | Performance Goals | SC-001 (indirect); operational SLA |
| Indexing completion < 30s per mutation | Performance Goals | SC-007 (one indexing cycle bound) |
| ≥ 85% line coverage (non-POJO) | Coverage Target | SC-009, FR-025, Constitution §III |

**Indexing failure handling**: Jobs retry with exponential backoff up to `app.rag.indexing.max-retries` (default 3). Ticket commits are never rolled back on indexing failure; operators use `/assistant/index-status/{ticketId}` to inspect pending/failed state (FR-027).

**Assistant dependency failures**: Embedding or LLM errors return HTTP 503 problem+json; no fabricated answers (FR-026, contracts/rag-api.yaml).

**Accessibility (v1)**: WCAG certification and i18n explicitly out of scope; basic semantic HTML and ARIA on loading/error states only (see spec Assumptions).

**Constraints**:
- RAG answers grounded only in retrieved ticket context
- `top-k` and similarity threshold externalized (no hard-coded values)
- Re-index reliable after searchable text changes
- ≥ 85% line coverage on non-POJO code

**Scale/Scope**: Single-tenant; ~10k tickets; 5 UI screens; 2 feature modules

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Backend Architecture & Modularity | PASS | Java 21, Spring Boot, constructor injection, MapStruct, package-by-feature, Liquibase |
| II. Persistence & Frontend | PASS | PostgreSQL, PGVector, React + TypeScript |
| III. Validation & State Ownership | PASS | Bean Validation + service-layer state machine; integration tests planned |
| IV. RAG Integrity | PASS | Grounding guard, structured sources, configurable top-k/threshold, re-index on text change |
| V. Security & Spec-First | PASS | Env-based secrets; spec + plan complete before implementation |

**Post-design re-check**: All gates pass.

## Architecture

### System Context

```text
┌─────────────┐     REST/JSON      ┌──────────────────────────────────────┐
│  React SPA  │ ◄───────────────► │         Spring Boot Backend          │
│  (Vite)     │                    │  ┌────────────┐    ┌──────────────┐  │
└─────────────┘                    │  │   ticket   │    │     rag      │  │
                                   │  │  feature   │    │   feature    │  │
                                   │  └─────┬──────┘    └──────┬───────┘  │
                                   │        │                   │          │
                                   │        ▼                   ▼          │
                                   │  ┌─────────────────────────────────┐  │
                                   │  │     PostgreSQL + PGVector       │  │
                                   │  │  (tickets, comments, vectors)   │  │
                                   │  └─────────────────────────────────┘  │
                                   └──────────────────┬───────────────────────┘
                                                      │ HTTPS
                                                      ▼
                                              ┌───────────────┐
                                              │  OpenAI API   │
                                              │ (embed+chat)  │
                                              └───────────────┘
```

### Module Boundaries

| Module | Responsibility | Must NOT |
|--------|----------------|----------|
| `ticket` | CRUD, comments, search, state machine, indexing job enqueue | Call LLM; embed text |
| `rag` | Canonical doc build, embed, vector upsert, retrieval, grounding, answer | Mutate ticket state |
| `shared` | Config, exceptions, ProblemDetail handler | Business logic |

Cross-module contract: `ticket` publishes `TicketChangedEvent`; `rag` listens and processes indexing jobs. No direct rag → ticket repository access.

### RAG Pipeline

```text
Ticket mutation (create/update/comment/status)
        │
        ▼
  IndexingJob (outbox, same TX)
        │
        ▼ (after commit)
  KnowledgeDocumentBuilder
  ── per unit: description | comment | resolution
  ── canonical text + text_hash
        │
        ├── text changed ──► EmbeddingModel.embed()
        │                         │
        │                         ▼
        │                   PgVectorStore.upsert()
        │
        └── metadata only ──► update vector metadata (no embed)

User question
        │
        ▼
  EmbeddingModel.embed(query)
        │
        ▼
  PgVectorStore.similaritySearch(topK)
        │
        ▼
  GroundingGuard.filter(threshold)
        │
        ├── 0 hits ──► fixed no-match response (no LLM)
        │
        └── N hits ──► ChatClient (retrieved context only)
                         │
                         ▼
                   AskResponse { answer, sources[] }
```

### Configuration (externalized)

```yaml
app:
  rag:
    retrieval:
      top-k: 5                    # APP_RAG_RETRIEVAL_TOP_K
      similarity-threshold: 0.75    # APP_RAG_RETRIEVAL_SIMILARITY_THRESHOLD
    no-match-message: "I could not find any relevant tickets in our history that answer this question."
    indexing:
      max-retries: 3
      poll-interval-ms: 2000
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        options:
          model: text-embedding-3-small
      chat:
        options:
          model: gpt-4o-mini
    vectorstore:
      pgvector:
        initialize-schema: false    # Liquibase owns DDL in prod
        table-name: knowledge_embeddings
        dimensions: 1536
        distance-type: COSINE_DISTANCE
        index-type: HNSW
```

## Project Structure

### Documentation (this feature)

```text
specs/001-support-ticket-rag/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── tickets-api.yaml
│   ├── rag-api.yaml
│   └── state-machine.md
└── tasks.md             # Phase 2 (/speckit-tasks)
```

### Source Code (repository root)

```text
backend/
├── pom.xml
├── src/main/java/com/ticketing/
│   ├── Application.java
│   ├── shared/
│   │   ├── config/          # Liquibase, OpenAPI, RAG properties
│   │   └── exception/       # GlobalExceptionHandler, ProblemDetail mappers
│   ├── ticket/
│   │   ├── api/             # TicketController, DTOs (records)
│   │   ├── domain/          # Ticket, Comment, TicketStateMachine, enums
│   │   ├── service/         # TicketService, CommentService
│   │   ├── repository/      # JPA repositories
│   │   └── event/           # TicketChangedEvent
│   └── rag/
│       ├── api/             # AssistantController
│       ├── ingestion/       # KnowledgeDocumentBuilder, IndexingJobProcessor
│       ├── retrieval/       # RetrievalService, GroundingGuard
│       ├── service/           # AssistantService
│       └── domain/          # KnowledgeDocument, IndexingJob entities
├── src/main/resources/
│   ├── application.yml
│   └── db/changelog/        # Liquibase master + changesets
└── src/test/java/
    ├── ticket/
    │   ├── domain/TicketStateMachineTest.java
    │   ├── service/TicketServiceTest.java
    │   └── api/TicketControllerTest.java
    └── rag/
        ├── retrieval/GroundingGuardTest.java
        └── integration/
            ├── TicketLifecycleIT.java
            └── RagRetrievalIT.java

frontend/
├── package.json
├── vite.config.ts
├── src/
│   ├── app/                 # Router, layout
│   ├── features/
│   │   ├── tickets/
│   │   │   ├── pages/       # TicketListPage, TicketDetailPage
│   │   │   ├── components/  # TicketForm, StatusActions, CommentList
│   │   │   └── api/         # ticketApi.ts
│   │   └── assistant/
│   │       ├── pages/       # AssistantPage
│   │       ├── components/  # QuestionForm, AnswerPanel, SourceList
│   │       └── api/         # assistantApi.ts
│   └── shared/
│       ├── api/client.ts    # fetch wrapper, problem+json parser
│       └── components/      # ErrorBanner, LoadingSpinner
└── src/test/                # Vitest component tests
```

**Structure Decision**: Web application with `backend/` + `frontend/` split. Package-by-feature inside backend (`ticket`, `rag`, `shared`). Frontend mirrors with `features/tickets` and `features/assistant`.

## UI Flow

### Screen Map

| Route | Screen | Primary Actions |
|-------|--------|-----------------|
| `/tickets` | Ticket List | Search, filter by status, create ticket, navigate to detail |
| `/tickets/new` | Create Ticket | Form: title, description, priority, category, assignee |
| `/tickets/:id` | Ticket Detail | View fields, add comment, edit fields, status transitions |
| `/assistant` | Knowledge Assistant | Ask question, view answer + source citations |

### Ticket List Flow

1. Load paginated tickets (`GET /api/v1/tickets`).
2. User types keyword → debounced search (`?q=`).
3. User selects status filter → `?status=`.
4. Click row → navigate to detail.
5. "New Ticket" → create form.

### Ticket Detail Flow

1. Load ticket + comments (`GET /api/v1/tickets/{id}`).
2. Edit fields → PATCH; show validation errors from problem+json.
3. Add comment → POST; append to list.
4. Status panel shows only valid next states (derived from current status).
5. RESOLVED transition opens resolution textarea (required).
6. Invalid transition → 409 error banner with `detail` message.

### Assistant Flow

1. User enters question → POST `/api/v1/assistant/ask`.
2. Loading state during retrieval + generation.
3. On success:
   - Render `answer` text.
   - Render `sources` as clickable links to `/tickets/{ticketId}` with `contentTypes` badges and matched-reasons text.
4. On no-match (`sources` empty): show answer text (fixed message), no source panel.
5. On error: show problem+json detail.

### Error Handling (UI)

- 400: inline field errors from `errors[]` array.
- 409: status transition banner.
- Network/500: generic retry banner.

## Test Strategy

### Coverage Target

≥ 85% line coverage on services, controllers, domain logic, React components/hooks. Exclude POJOs, entities, mappers, config.

### Backend Test Matrix

| Layer | Framework | Scope |
|-------|-----------|-------|
| Unit | JUnit 5 + Mockito | `TicketStateMachine` (all valid/invalid transitions), `GroundingGuard`, `KnowledgeDocumentBuilder` |
| Web | `@WebMvcTest` | Controller status codes, problem+json shape, `@Valid` rejection |
| Integration | `@SpringBootTest` + Testcontainers | Full ticket lifecycle; RAG index + query; re-index after comment |
| Repository | `@DataJpaTest` | Keyword search query, ticket sequence |

### State Machine Tests (mandatory cases)

- Every valid transition in transition table → success
- Every invalid transition → `InvalidStateTransitionException` / 409
- RESOLVED without resolution → 400
- Terminal state any transition → 409
- Whitespace-only resolution → 400

### RAG Tests (mandatory cases)

| Case | Assert |
|------|--------|
| Indexed ticket, matching question | `sources` non-empty; each entry has `ticketId`, `displayId`, and non-empty `contentTypes[]` |
| No indexed tickets | Fixed no-match message; `sources: []` |
| Below threshold | No-match response; no LLM call (verify with mock) |
| Post-comment re-index | Answer includes new comment content |
| Metadata-only status change | Retrieval still finds ticket; no re-embed (hash unchanged) |
| Out-of-domain question | No-match; no fabricated facts |

### Frontend Tests

- TicketForm: validation messages, disabled submit on empty title
- StatusActions: resolution field visible only for RESOLVED transition
- AssistantPage: renders sources; empty sources shows no-match state
- API error: problem+json `detail` displayed

### CI

```bash
# backend
./mvnw verify -Pintegration-tests

# frontend
npm test -- --coverage
```

## Evaluation Strategy (RAG Quality)

### Offline Evaluation Dataset

Build `backend/src/test/resources/rag-eval/cases.json`:

```json
[
  {
    "seedTickets": ["payment-failure-1.json"],
    "question": "Have we seen payment failures before?",
    "expectSourcesMin": 1,
    "expectTicketIds": ["TKT-1001"],
    "expectAnswerContains": ["payment"]
  }
]
```

10–15 cases covering: exact ticket lookup, thematic search, no-match, post-update freshness.

### Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Citation completeness | 100% | Every grounded response has `sources` with both fields |
| No-match precision | 100% | Out-of-domain questions return empty `sources` |
| Retrieval recall | ≥ 90% | Eval cases: expected ticket appears in retrieved set |
| Freshness | 100% | Post-mutation question reflects new content within 1 index cycle |
| Hallucination rate | 0% | Manual review: no ticket facts absent from sources |

### Evaluation Runs

1. **CI integration** (`RagEvalIT`): seed DB, run cases, assert structural metrics.
2. **Manual review** (pre-release): 20 ad-hoc questions against seeded demo data.
3. **Threshold tuning**: sweep `similarity-threshold` 0.65–0.85 on eval set; document chosen default in `research.md`.

### Regression

Any RAG pipeline change must re-run eval suite. Threshold/default changes require eval report note in PR.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Outbox indexing jobs | Reliable re-index after ticket commit | Sync embed blocks writes; naked `@Async` loses events on crash |
| Separate `ticket` / `rag` packages | Enforce domain isolation per user requirement | Layer-only layout allows RAG logic in ticket services |

## Phase Artifacts

| Artifact | Path | Status |
|----------|------|--------|
| Research | `research.md` | Complete |
| Data model | `data-model.md` | Complete |
| Ticket API contract | `contracts/tickets-api.yaml` | Complete |
| RAG API contract | `contracts/rag-api.yaml` | Complete |
| State machine | `contracts/state-machine.md` | Complete |
| Quickstart | `quickstart.md` | Complete |
| Tasks | `tasks.md` | Pending (`/speckit-tasks`) |
