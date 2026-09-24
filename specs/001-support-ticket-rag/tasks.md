---
description: "Task list for AI-Powered Support Ticket Management feature implementation"
---

# Tasks: AI-Powered Support Ticket Management

**Input**: Design documents from `/specs/001-support-ticket-rag/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Not included — spec defines acceptance scenarios but does not request TDD test-generation tasks. Constitution mandates tests at implementation time; add during `/speckit-implement` as needed.

**Organization**: Tasks grouped by user story for independent implementation and validation per quickstart.md scenarios.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: User story label (US1–US5)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `backend/src/main/java/com/ticketing/`
- **Frontend**: `frontend/src/`
- **Migrations**: `backend/src/main/resources/db/changelog/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize backend and frontend projects per plan.md structure

- [ ] T001 Create backend Maven project structure in `backend/` with `pom.xml` (Java 21, Spring Boot 3.5.x parent, Spring AI 1.1.x BOM)
- [ ] T002 Add backend dependencies in `backend/pom.xml`: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-validation, liquibase-core, mapstruct, lombok, springdoc-openapi, spring-ai-starter-vector-store-pgvector, spring-ai-starter-model-openai, spring-boot-starter-actuator
- [ ] T003 [P] Create Spring Boot entry point `backend/src/main/java/com/ticketing/Application.java`
- [ ] T004 [P] Create frontend Vite + React 19 + TypeScript project in `frontend/` with `package.json` and `vite.config.ts`
- [ ] T005 [P] Configure frontend dev proxy to backend in `frontend/vite.config.ts` (`/api` → `http://localhost:8080`)
- [ ] T006 [P] Add Maven wrapper `backend/mvnw` and `backend/.mvn/wrapper/maven-wrapper.properties`
- [ ] T007 [P] Create `docker-compose.yml` at repo root with `pgvector/pgvector:pg16` service (db: ticketing, port 5432)
- [ ] T008 [P] Create backend package directories: `ticket/`, `rag/`, `shared/` under `backend/src/main/java/com/ticketing/`
- [ ] T009 [P] Create frontend feature directories: `frontend/src/features/tickets/`, `frontend/src/features/assistant/`, `frontend/src/shared/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema, shared config, and cross-cutting infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T010 Create Liquibase master changelog `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
- [ ] T011 [P] Add changeset `backend/src/main/resources/db/changelog/changes/001-extensions.sql` (`CREATE EXTENSION IF NOT EXISTS vector`)
- [ ] T012 [P] Add changeset `backend/src/main/resources/db/changelog/changes/002-ticket-tables.sql` (ticket table: `id` UUID PK, `display_id` VARCHAR(20) UNIQUE NOT NULL, `title` VARCHAR(200) NOT NULL, `description` TEXT NOT NULL, `status` ENUM default OPEN, `priority` ENUM NOT NULL, `assignee` VARCHAR(100) nullable, `category` ENUM NOT NULL default GENERAL, `resolution` TEXT nullable, `created_at`/`updated_at` TIMESTAMPTZ NOT NULL; comment table: `id` UUID PK, `ticket_id` UUID FK NOT NULL, `content` TEXT NOT NULL, `author` VARCHAR(100) NOT NULL, `created_at` TIMESTAMPTZ NOT NULL; display_id sequence)
- [ ] T013 [P] Add changeset `backend/src/main/resources/db/changelog/changes/003-knowledge-tables.sql` (knowledge_document + indexing_job tables per data-model.md with unique constraint on `(ticket_id, content_type, source_ref_id)`)
- [ ] T014 [P] Add changeset `backend/src/main/resources/db/changelog/changes/004-search-indexes.sql` (indexes on status, priority, category; GIN/trigram on title + description)
- [ ] T015 [P] Add changeset `backend/src/main/resources/db/changelog/changes/005-vector-table.sql` (knowledge_embeddings PGVector table, HNSW index, 1536 dimensions, COSINE_DISTANCE)
- [ ] T016 Configure datasource and Liquibase in `backend/src/main/resources/application.yml` (PostgreSQL URL, `spring.liquibase.change-log`, `spring.ai.vectorstore.pgvector.initialize-schema: false`)
- [ ] T017 [P] Create `RagProperties` config record in `backend/src/main/java/com/ticketing/shared/config/RagProperties.java` (`top-k`, `similarity-threshold`, `no-match-message`, indexing retry settings bound to `app.rag.*` env vars)
- [ ] T018 [P] Create OpenAPI config in `backend/src/main/java/com/ticketing/shared/config/OpenApiConfig.java` (title, version, `/api/v1` base)
- [ ] T019 Create `GlobalExceptionHandler` in `backend/src/main/java/com/ticketing/shared/exception/GlobalExceptionHandler.java` (problem+json for 400 validation, 404 not found, 409 state conflict, 500 generic)
- [ ] T020 [P] Create domain exception types in `backend/src/main/java/com/ticketing/shared/exception/` (`ResourceNotFoundException`, `InvalidStateTransitionException`, `ValidationException`)
- [ ] T021 [P] Create ticket enums in `backend/src/main/java/com/ticketing/ticket/domain/` (`TicketStatus`: OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED; `Priority`: LOW, MEDIUM, HIGH; `Category`: PAYMENT, SHIPMENT, ACCOUNT, GENERAL)
- [ ] T022 Create `TicketChangedEvent` record in `backend/src/main/java/com/ticketing/ticket/event/TicketChangedEvent.java` (ticketId, trigger enum: CREATE, UPDATE, COMMENT_ADD, COMMENT_UPDATE, COMMENT_DELETE, STATUS_CHANGE)
- [ ] T023 [P] Create frontend API client in `frontend/src/shared/api/client.ts` (fetch wrapper, problem+json parser, base URL `/api/v1`)
- [ ] T024 [P] Create shared UI components in `frontend/src/shared/components/ErrorBanner.tsx` and `frontend/src/shared/components/LoadingSpinner.tsx`
- [ ] T025 Create app router shell in `frontend/src/app/App.tsx` and `frontend/src/app/router.tsx` (routes: `/tickets`, `/tickets/new`, `/tickets/:id`, `/assistant`; layout with nav links)

**Checkpoint**: Foundation ready — database migrates on startup, shared error handling and routing in place

---

## Phase 3: User Story 1 — Create and Track Support Tickets (Priority: P1) 🎯 MVP

**Goal**: Agents can create tickets (title, description, priority), list tickets, and view full detail with status OPEN by default

**Independent Test**: Run quickstart Scenario 1 — create ticket, list shows displayId/title/status/priority, detail shows all fields and empty comments

### Implementation for User Story 1

- [ ] T026 [P] [US1] Create JPA `Ticket` entity in `backend/src/main/java/com/ticketing/ticket/domain/Ticket.java` (fields per data-model.md; `title` VARCHAR(200) trimmed non-empty; `status` default OPEN; `category` default GENERAL)
- [ ] T027 [P] [US1] Create JPA `Comment` entity in `backend/src/main/java/com/ticketing/ticket/domain/Comment.java` (`content` TEXT non-blank, `author` VARCHAR(100) NOT NULL, `created_at` TIMESTAMPTZ)
- [ ] T028 [US1] Create `TicketRepository` in `backend/src/main/java/com/ticketing/ticket/repository/TicketRepository.java` (JpaRepository + display_id sequence query)
- [ ] T029 [US1] Create `CommentRepository` in `backend/src/main/java/com/ticketing/ticket/repository/CommentRepository.java`
- [ ] T030 [P] [US1] Create request/response DTO records in `backend/src/main/java/com/ticketing/ticket/api/` (`CreateTicketRequest`, `TicketSummary`, `TicketDetail`, `CommentDto`, `TicketPage` per contracts/tickets-api.yaml)
- [ ] T031 [US1] Create MapStruct mapper `TicketMapper` in `backend/src/main/java/com/ticketing/ticket/api/TicketMapper.java`
- [ ] T032 [US1] Implement `TicketService` in `backend/src/main/java/com/ticketing/ticket/service/TicketService.java` (create with display_id generation TKT-NNNN, list paginated, getById with comments chronological; publish `TicketChangedEvent` on create)
- [ ] T033 [US1] Implement `TicketController` in `backend/src/main/java/com/ticketing/ticket/api/TicketController.java` (`POST /api/v1/tickets` → 201 + Location, `GET /api/v1/tickets` paginated, `GET /api/v1/tickets/{ticketId}`)
- [ ] T034 [P] [US1] Create ticket API client in `frontend/src/features/tickets/api/ticketApi.ts` (listTickets, getTicket, createTicket)
- [ ] T035 [P] [US1] Create `TicketListPage` in `frontend/src/features/tickets/pages/TicketListPage.tsx` (table: displayId, title, status, priority; link to detail; "New Ticket" button)
- [ ] T036 [P] [US1] Create `CreateTicketPage` in `frontend/src/features/tickets/pages/CreateTicketPage.tsx` (form: title, description, priority, category, optional assignee)
- [ ] T037 [US1] Create `TicketForm` component in `frontend/src/features/tickets/components/TicketForm.tsx` (client-side required field validation, submit via ticketApi)
- [ ] T038 [US1] Create `TicketDetailPage` in `frontend/src/features/tickets/pages/TicketDetailPage.tsx` (read-only view: title, description, status, priority, assignee, category, comments list)

**Checkpoint**: User Story 1 complete — ticket CRUD (create/list/detail) works end-to-end via API and UI

---

## Phase 4: User Story 2 — Update Tickets and Collaborate via Comments (Priority: P2)

**Goal**: Agents can update ticket fields, add comments with author/timestamp, and see validation errors for invalid input

**Independent Test**: Run quickstart Scenario 2 — update title, add comment, empty title returns 400 problem+json

### Implementation for User Story 2

- [ ] T039 [US2] Add `updateTicket` method to `TicketService` in `backend/src/main/java/com/ticketing/ticket/service/TicketService.java` (PATCH fields: title, description, priority, assignee, category; reject blank/whitespace-only title; publish `TicketChangedEvent` trigger UPDATE)
- [ ] T040 [US2] Implement `CommentService` in `backend/src/main/java/com/ticketing/ticket/service/CommentService.java` (add comment: content non-blank, author required; publish `TicketChangedEvent` trigger COMMENT_ADD)
- [ ] T041 [US2] Add `PATCH /api/v1/tickets/{ticketId}` and `POST /api/v1/tickets/{ticketId}/comments` to `TicketController` in `backend/src/main/java/com/ticketing/ticket/api/TicketController.java` (400 on Bean Validation failure with problem+json `errors[]`)
- [ ] T042 [P] [US2] Create `UpdateTicketRequest` and `CreateCommentRequest` records in `backend/src/main/java/com/ticketing/ticket/api/` per contracts/tickets-api.yaml
- [ ] T043 [P] [US2] Add `updateTicket` and `addComment` to `frontend/src/features/tickets/api/ticketApi.ts`
- [ ] T044 [US2] Add editable fields to `TicketDetailPage` in `frontend/src/features/tickets/pages/TicketDetailPage.tsx` (inline edit or edit mode for title, description, priority, assignee, category)
- [ ] T045 [P] [US2] Create `CommentList` component in `frontend/src/features/tickets/components/CommentList.tsx` (chronological display: content, author, timestamp)
- [ ] T046 [US2] Create `CommentForm` component in `frontend/src/features/tickets/components/CommentForm.tsx` (content + author fields, submit adds comment and refreshes list)
- [ ] T047 [US2] Add inline validation error display for 400 responses in `frontend/src/features/tickets/pages/TicketDetailPage.tsx` (map problem+json `errors[]` to form fields)

**Checkpoint**: User Story 2 complete — updates and comments persist; invalid input shows meaningful errors

---

## Phase 5: User Story 3 — Enforce Ticket Lifecycle (Priority: P2)

**Goal**: Server-enforced state machine accepts only valid transitions; RESOLVED requires non-blank resolution field; invalid transitions return 409

**Independent Test**: Run quickstart Scenario 3 — valid transitions succeed; CLOSED→OPEN returns 409; RESOLVED without resolution returns 400

### Implementation for User Story 3

- [ ] T048 [US3] Implement pure `TicketStateMachine` in `backend/src/main/java/com/ticketing/ticket/domain/TicketStateMachine.java` (`canTransition(from,to)` graph lookup; `transition(ticket,target,resolution)` validates resolution non-blank when target RESOLVED; throws `InvalidStateTransitionException` per contracts/state-machine.md)
- [ ] T049 [US3] Add `transitionStatus` method to `TicketService` in `backend/src/main/java/com/ticketing/ticket/service/TicketService.java` (delegate to state machine; persist resolution on RESOLVED; publish `TicketChangedEvent` trigger STATUS_CHANGE)
- [ ] T050 [US3] Add `PATCH /api/v1/tickets/{ticketId}/status` to `TicketController` in `backend/src/main/java/com/ticketing/ticket/api/TicketController.java` (`StatusTransitionRequest` with status + optional resolution; 409 on invalid transition, 400 on blank resolution)
- [ ] T051 [P] [US3] Create `StatusTransitionRequest` record in `backend/src/main/java/com/ticketing/ticket/api/StatusTransitionRequest.java`
- [ ] T052 [P] [US3] Create `StatusActions` component in `frontend/src/features/tickets/components/StatusActions.tsx` (show only valid next states from current status; resolution textarea required when transitioning to RESOLVED)
- [ ] T053 [US3] Integrate `StatusActions` into `TicketDetailPage` in `frontend/src/features/tickets/pages/TicketDetailPage.tsx` (409 error banner with problem+json `detail`; refresh ticket on success)
- [ ] T054 [P] [US3] Add `transitionStatus` to `frontend/src/features/tickets/api/ticketApi.ts`

**Checkpoint**: User Story 3 complete — lifecycle transitions enforced server-side with clear UI feedback

---

## Phase 6: User Story 4 — Find Tickets by Search and Filter (Priority: P3)

**Goal**: Agents search tickets by keyword (title + description) and filter by status; empty results show zero state without error

**Independent Test**: Run quickstart Scenario 4 — keyword match, status filter, no-match returns total 0

### Implementation for User Story 4

- [ ] T055 [US4] Add keyword search query to `TicketRepository` in `backend/src/main/java/com/ticketing/ticket/repository/TicketRepository.java` (trigram/GIN match on title and description only; case-insensitive)
- [ ] T056 [US4] Extend `TicketService.listTickets` in `backend/src/main/java/com/ticketing/ticket/service/TicketService.java` (accept optional `q` and `status` query params; combine filters; return empty page not error when no matches)
- [ ] T057 [US4] Add `q` and `status` query parameters to `GET /api/v1/tickets` in `TicketController` per contracts/tickets-api.yaml
- [ ] T058 [P] [US4] Create `SearchBar` component in `frontend/src/features/tickets/components/SearchBar.tsx` (debounced keyword input)
- [ ] T059 [P] [US4] Create `StatusFilter` component in `frontend/src/features/tickets/components/StatusFilter.tsx` (dropdown: OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED, All)
- [ ] T060 [US4] Integrate search and filter into `TicketListPage` in `frontend/src/features/tickets/pages/TicketListPage.tsx` (pass `q` and `status` to listTickets; show empty state when total is 0)

**Checkpoint**: User Story 4 complete — search and filter work independently of other stories

---

## Phase 7: User Story 5 — Ask Questions Over Ticket History (Priority: P3)

**Goal**: RAG assistant answers from indexed ticket content with structured sources; no-match returns fixed message and empty sources; re-index on text changes

**Independent Test**: Run quickstart Scenario 5 — grounded answer with sources; no-match for out-of-domain; post-comment re-ask reflects new content

### Implementation for User Story 5

- [ ] T061 [P] [US5] Create JPA `KnowledgeDocument` entity in `backend/src/main/java/com/ticketing/rag/domain/KnowledgeDocument.java` (fields per data-model.md; `content_type` ENUM DESCRIPTION/COMMENT/RESOLUTION; `text_hash` SHA-256 VARCHAR(64); unique on ticket_id + content_type + source_ref_id)
- [ ] T062 [P] [US5] Create JPA `IndexingJob` entity in `backend/src/main/java/com/ticketing/rag/domain/IndexingJob.java` (trigger ENUM, status PENDING/PROCESSING/COMPLETED/FAILED, attempts default 0, max 3 retries)
- [ ] T063 [US5] Create `KnowledgeDocumentRepository` and `IndexingJobRepository` in `backend/src/main/java/com/ticketing/rag/repository/`
- [ ] T064 [US5] Implement `KnowledgeDocumentBuilder` in `backend/src/main/java/com/ticketing/rag/ingestion/KnowledgeDocumentBuilder.java` (canonical text format per data-model.md; one unit per description, each comment, resolution when present; compute text_hash)
- [ ] T065 [US5] Implement `IndexingJobEnqueuer` in `backend/src/main/java/com/ticketing/rag/ingestion/IndexingJobEnqueuer.java` (create PENDING job in same transaction as ticket mutation via outbox pattern)
- [ ] T066 [US5] Create `TicketChangedEventListener` in `backend/src/main/java/com/ticketing/rag/ingestion/TicketChangedEventListener.java` (listen after commit; enqueue indexing job)
- [ ] T067 [US5] Implement `IndexingJobProcessor` in `backend/src/main/java/com/ticketing/rag/ingestion/IndexingJobProcessor.java` (poll pending jobs; re-embed when text_hash changes; metadata-only update without re-embed; upsert to PgVectorStore; exponential backoff max 3 retries)
- [ ] T068 [US5] Configure Spring AI `PgVectorStore` and `EmbeddingModel` in `backend/src/main/java/com/ticketing/shared/config/VectorStoreConfig.java` (table knowledge_embeddings, 1536 dimensions, metadata keys: knowledgeDocumentId, ticketId, displayId, contentType, status, priority, category)
- [ ] T069 [US5] Implement `RetrievalService` in `backend/src/main/java/com/ticketing/rag/retrieval/RetrievalService.java` (embed query; similaritySearch with configurable top-k from RagProperties)
- [ ] T070 [US5] Implement `GroundingGuard` in `backend/src/main/java/com/ticketing/rag/retrieval/GroundingGuard.java` (filter results below similarity-threshold; return empty list when no hits pass)
- [ ] T071 [US5] Implement `AssistantService` in `backend/src/main/java/com/ticketing/rag/service/AssistantService.java` (single retrieve-then-answer flow; no LLM call when grounding guard returns empty; constrained prompt using retrieved context only; build sources array with displayId as ticketId + contentType per contracts/rag-api.yaml; fixed no-match message from RagProperties)
- [ ] T072 [US5] Implement `AssistantController` in `backend/src/main/java/com/ticketing/rag/api/AssistantController.java` (`POST /api/v1/assistant/ask`, `GET /api/v1/assistant/index-status/{ticketId}` per contracts/rag-api.yaml; 503 on embedding/LLM failure)
- [ ] T073 [P] [US5] Create assistant DTO records in `backend/src/main/java/com/ticketing/rag/api/` (`AskRequest` question minLength 3 maxLength 2000, `AskResponse`, `Source`, `IndexStatusResponse`)
- [ ] T074 [P] [US5] Create assistant API client in `frontend/src/features/assistant/api/assistantApi.ts` (askQuestion, getIndexStatus)
- [ ] T075 [P] [US5] Create `QuestionForm` component in `frontend/src/features/assistant/components/QuestionForm.tsx`
- [ ] T076 [P] [US5] Create `AnswerPanel` component in `frontend/src/features/assistant/components/AnswerPanel.tsx` (render answer text; hide source panel when sources empty)
- [ ] T077 [P] [US5] Create `SourceList` component in `frontend/src/features/assistant/components/SourceList.tsx` (clickable links to `/tickets/{id}` with contentType badge)
- [ ] T078 [US5] Create `AssistantPage` in `frontend/src/features/assistant/pages/AssistantPage.tsx` (question form, loading state, answer + sources, error banner on failure)
- [ ] T079 [US5] Wire `TicketChangedEvent` publishing in `TicketService` and `CommentService` for all mutation paths (create, update, comment add, status change) to trigger indexing pipeline

**Checkpoint**: User Story 5 complete — RAG Q&A grounded with citations; no-match honest; re-index after content changes

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation validation, CORS, scheduling, and production readiness

- [ ] T080 [P] Enable CORS for frontend dev origin in `backend/src/main/java/com/ticketing/shared/config/WebConfig.java` (`http://localhost:5173`)
- [ ] T081 [P] Add Spring scheduling config for `IndexingJobProcessor` poll interval in `backend/src/main/java/com/ticketing/shared/config/SchedulingConfig.java` (`app.rag.indexing.poll-interval-ms`)
- [ ] T082 [P] Add actuator health endpoint exposure in `backend/src/main/resources/application.yml` (verify per quickstart.md)
- [ ] T083 Create `.env.example` at repo root documenting required env vars (OPENAI_API_KEY, datasource, APP_RAG_RETRIEVAL_TOP_K, APP_RAG_RETRIEVAL_SIMILARITY_THRESHOLD)
- [ ] T084 [P] Add navigation link to Assistant in `frontend/src/app/App.tsx` layout
- [ ] T085 Run quickstart.md Scenarios 1–7 validation and fix any gaps found
- [ ] T086 [P] Add README.md at repo root with setup instructions referencing quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — **BLOCKS all user stories**
- **User Stories (Phase 3–7)**: All depend on Foundational completion
  - US1 (P1) → US2 (P2) → US3 (P2) → US4 (P3) → US5 (P3) recommended sequential order
  - US4 can parallel with US3 after US1 complete (search only needs ticket list)
  - US5 depends on US1–US2 for ticket content to index; can start backend RAG module after Foundational while US3/US4 proceed
- **Polish (Phase 8)**: Depends on desired user stories being complete

### User Story Dependencies

| Story | Depends On | Can Start After |
|-------|-----------|-----------------|
| US1 (P1) | Foundational | Phase 2 complete |
| US2 (P2) | US1 (ticket exists to update) | Phase 3 checkpoint |
| US3 (P2) | US1 (ticket to transition) | Phase 3 checkpoint (parallel with US2 possible) |
| US4 (P3) | US1 (tickets to search) | Phase 3 checkpoint (parallel with US2/US3) |
| US5 (P3) | US1 + US2 (content to index) | Phase 4 checkpoint minimum |

### Within Each User Story

- Domain entities before repositories
- Repositories before services
- Services before controllers
- Backend endpoints before frontend API client
- API client before UI pages/components

### Parallel Opportunities

- **Phase 1**: T003–T009 all parallel after T001–T002
- **Phase 2**: T011–T015 migrations parallel; T017–T018, T020–T021, T023–T024 parallel after T010
- **Phase 3**: T026–T027 entities parallel; T034–T036 frontend parallel after T033
- **Phase 5**: T051–T052, T054 parallel
- **Phase 6**: T058–T059 parallel
- **Phase 7**: T061–T062 entities parallel; T073–T077 frontend parallel after T072
- **Phase 8**: T080–T082, T084, T086 parallel

---

## Parallel Example: User Story 1

```bash
# Backend entities in parallel:
T026: Ticket entity in backend/.../ticket/domain/Ticket.java
T027: Comment entity in backend/.../ticket/domain/Comment.java

# Frontend pages in parallel (after T033 controller):
T035: TicketListPage
T036: CreateTicketPage
```

---

## Parallel Example: User Story 5

```bash
# RAG domain entities in parallel:
T061: KnowledgeDocument entity
T062: IndexingJob entity

# Frontend assistant components in parallel (after T072):
T075: QuestionForm
T076: AnswerPanel
T077: SourceList
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: quickstart Scenario 1
5. Demo ticket create/list/detail

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → Scenario 1 → **MVP demo**
3. US2 → Scenario 2 → collaborative updates
4. US3 → Scenario 3 → lifecycle integrity
5. US4 → Scenario 4 → search efficiency
6. US5 → Scenario 5 → AI differentiator
7. Polish → full quickstart validation

### Parallel Team Strategy

With multiple developers after Phase 2:

- **Developer A**: US1 → US2 (ticket core)
- **Developer B**: US3 → US4 (lifecycle + search, after US1 entities exist)
- **Developer C**: US5 backend RAG pipeline (after Foundational, parallel with US2–US4)
- **Developer D**: Frontend across stories as backend endpoints land

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks in same phase
- [Story] label maps task to user story for traceability
- Each user story independently testable via quickstart.md scenarios
- Constitution §IV (RAG Integrity) is non-negotiable — US5 tasks T070–T071 enforce grounding guard and structured sources
- Constitution §III requires backend-owned state machine — US3 task T048 is pure domain, no Spring dependency
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
