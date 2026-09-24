# Feature Specification: AI-Powered Support Ticket Management

**Feature Branch**: `001-support-ticket-rag`

**Created**: 2025-09-25

**Status**: Draft

**Input**: User description: "Build an AI-powered support ticket management system with core ticket CRUD, enforced lifecycle state machine, and RAG-based natural-language Q&A over ticket history—grounded strictly in real ticket data with citations."

## Clarifications

### Session 2025-09-25

- Q: When a ticket moves to RESOLVED, how must resolution notes be captured? → A: Dedicated resolution field required when status becomes RESOLVED.
- Q: How should ticket content be split into RAG knowledge documents? → A: Separate indexed units per description, each comment, and resolution field.
- Q: What structure must the assistant response use to cite source tickets? → A: Structured `sources` array with one entry per ticket (`ticketId` UUID, `displayId`, and `contentTypes[]` listing matched unit types: description, comment, resolution).
- Q: Which ticket changes must trigger re-indexing of knowledge documents? → A: Re-embed on searchable text change; metadata-only changes update unit metadata without re-embedding.
- Q: When retrieval finds no relevant tickets, what must the assistant response contain? → A: Fixed no-match message in answer; sources is an empty array.

### Session 2026-09-25

- Q: In each assistant `sources` entry, should `ticketId` be the internal UUID, the human-readable display ID, or both? → A: Both fields: `ticketId` (UUID for navigation/API) and `displayId` (e.g. TKT-1001 for human-readable citation).
- Q: After a comment is added to a ticket, can agents edit or delete that comment in v1? → A: Add-only — comments cannot be edited or deleted after creation.
- Q: Can support agents delete tickets in v1? → A: No delete — tickets are permanent once created; use CANCELLED status to abandon.
- Q: Must v1 include automated tests for the state machine and RAG grounding before the feature is considered complete? → A: Yes — automated tests required for state machine, RAG grounding/citations, and core API flows before v1 is done.
- Q: When creating a ticket, must the agent select a category, or should category default when omitted? → A: Optional — defaults to GENERAL when not specified on create.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and Track Support Tickets (Priority: P1)

A support agent creates a ticket when a customer reports an issue. They enter a title, description, priority, and optional assignee. They can list all tickets and open any ticket to view its full detail including status and comment history.

**Why this priority**: Ticket creation and visibility are the foundation of the product. Without them, no other workflow or AI feature delivers value.

**Independent Test**: Create a ticket, confirm it appears in the ticket list, open it, and verify all submitted fields display correctly.

**Acceptance Scenarios**:

1. **Given** no tickets exist, **When** an agent submits a new ticket with title, description, and priority, **Then** the system creates the ticket with status OPEN and displays it in the ticket list.
2. **Given** tickets exist, **When** an agent opens the ticket list, **Then** each ticket shows at minimum its identifier, title, status, and priority.
3. **Given** a ticket exists, **When** an agent views ticket detail, **Then** the system shows title, description, status, priority, assignee, and all comments in chronological order.

---

### User Story 2 - Update Tickets and Collaborate via Comments (Priority: P2)

A support agent updates ticket fields as work progresses and adds comments to record investigation steps, customer communication, and resolution notes. Invalid input is rejected with clear feedback.

**Why this priority**: Day-to-day support work depends on keeping ticket data current and maintaining a shared comment thread.

**Independent Test**: Update title, description, priority, and assignee on an existing ticket, add comments, and confirm changes persist after reload.

**Acceptance Scenarios**:

1. **Given** an OPEN ticket, **When** an agent updates title, description, priority, or assignee with valid values, **Then** the system saves the changes and shows the updated values on ticket detail.
2. **Given** a ticket exists, **When** an agent adds a comment, **Then** the comment appears on the ticket with author and timestamp and remains after the page is reloaded.
3. **Given** an agent submits invalid input (e.g., empty title or invalid priority), **When** the update is attempted, **Then** the system rejects the change and displays a meaningful error message in the interface.

---

### User Story 3 - Enforce Ticket Lifecycle (Priority: P2)

A support agent moves tickets through defined lifecycle states. The system accepts only valid transitions and blocks invalid ones, regardless of what the interface sends.

**Why this priority**: Lifecycle integrity prevents tickets from entering inconsistent states and is a core business rule of support operations.

**Independent Test**: Apply each valid transition and confirm success; attempt each invalid transition and confirm rejection with a clear error.

**Acceptance Scenarios**:

1. **Given** a ticket in OPEN status, **When** an agent transitions it to IN_PROGRESS, **Then** the status updates and the change persists.
2. **Given** a ticket in IN_PROGRESS status, **When** an agent transitions it to RESOLVED with a non-empty resolution field, **Then** the status updates and the resolution is stored on the ticket; when transitioned again to CLOSED, **Then** the ticket reaches a terminal state.
3. **Given** a ticket in IN_PROGRESS status, **When** an agent attempts to transition to RESOLVED without providing resolution notes, **Then** the system rejects the transition and displays a meaningful error.
4. **Given** a ticket in OPEN or IN_PROGRESS status, **When** an agent cancels it, **Then** the status becomes CANCELLED and no further forward progress is allowed.
5. **Given** a ticket in CLOSED or CANCELLED status, **When** an agent attempts to reopen or move it to OPEN, **Then** the system rejects the transition and returns a meaningful error.
6. **Given** a ticket in RESOLVED status, **When** an agent attempts to move it back to OPEN or IN_PROGRESS, **Then** the system rejects the transition.

**Valid transitions**:

| From | Allowed To |
|------|------------|
| OPEN | IN_PROGRESS, CANCELLED |
| IN_PROGRESS | RESOLVED, CANCELLED |
| RESOLVED | CLOSED |
| CLOSED | *(terminal)* |
| CANCELLED | *(terminal)* |

---

### User Story 4 - Find Tickets by Search and Filter (Priority: P3)

A support agent searches tickets by keyword and filters the list by status to locate related or active work quickly.

**Why this priority**: Search and filter improve agent efficiency once a meaningful volume of tickets exists.

**Independent Test**: Create tickets with distinct titles and statuses, search by keyword, filter by status, and confirm results match expectations.

**Acceptance Scenarios**:

1. **Given** multiple tickets with different titles and descriptions, **When** an agent searches by a keyword present in a ticket title or description, **Then** matching tickets appear in results.
2. **Given** tickets in multiple statuses, **When** an agent filters by a specific status, **Then** only tickets in that status are shown.
3. **Given** no tickets match the search keyword, **When** an agent runs the search, **Then** the system shows an empty result state without error.

---

### User Story 5 - Ask Questions Over Ticket History (Priority: P3)

A support agent asks natural-language questions about past tickets (e.g., "Have we seen payment failures before?" or "What was the resolution for ticket TKT-1001?"). The system answers using only relevant ticket content, cites the ticket IDs used, and states clearly when nothing relevant is found.

**Why this priority**: This is the AI-native differentiator—turning historical ticket data into actionable knowledge without manual search across many records.

**Independent Test**: Index tickets with known content, ask in-scope questions and confirm grounded answers with citations; ask out-of-scope questions and confirm an honest no-match response.

**Acceptance Scenarios**:

1. **Given** indexed tickets about payment failures, **When** an agent asks "Have we seen payment failures before?", **Then** the system returns an answer grounded in matching ticket content and cites each matching ticket once in `sources` with `ticketId` (UUID), `displayId`, and a non-empty `contentTypes` array.
2. **Given** a resolved ticket TKT-1001 with resolution notes, **When** an agent asks "What was the resolution for ticket TKT-1001?", **Then** the system returns the resolution from that ticket and cites it in `sources` with `displayId` TKT-1001, the corresponding `ticketId` UUID, and `contentTypes` including `resolution`.
3. **Given** no tickets relate to the question topic, **When** an agent asks an in-domain support question with no matches, **Then** the system returns a fixed no-match message in `answer`, an empty `sources` array, and no fabricated ticket facts.
4. **Given** a ticket is updated with new comments or resolution notes, **When** an agent asks a question about that content, **Then** the answer reflects the latest ticket information.

**Example questions the assistant should handle**:

- "Have we seen payment failures before?"
- "What was the resolution for ticket TKT-1001?"
- "What are the common causes of shipment tracking issues?"
- "Show me similar resolved tickets."
- "Which high-priority tickets are related to payment?"

---

### Edge Cases

- What happens when an agent submits a ticket with only whitespace in the title? System rejects with a validation error.
- What happens when an agent tries to assign a ticket to an empty assignee? System accepts unassigned state or rejects based on validation rules consistently.
- What happens when search keyword matches no field? System returns empty results, not an error.
- What happens when an agent asks the assistant a question while no tickets exist? System returns a fixed no-match message in `answer` and an empty `sources` array.
- What happens when a ticket is updated immediately before a question is asked? Answer reflects re-indexed content, not stale data; metadata-only updates (e.g., assignee change) appear in retrieval filters without re-embedding unchanged text.
- What happens when retrieval finds marginally related tickets below the relevance threshold? System returns a fixed no-match message in `answer` and an empty `sources` array rather than producing a weak or speculative answer (same response shape as zero-index scenarios; distinguished operationally by non-empty vector index).
- What happens when retrieved chunks lack citation metadata or resolvable ticket UUID? System MUST NOT call the LLM; it returns the fixed no-match message and an empty `sources` array.
- What happens when embedding or LLM services are unavailable? System returns HTTP 503 with a problem+json error; the UI shows a retry-friendly message.
- What happens when an indexing job fails after a ticket mutation commits? The ticket mutation remains persisted; the job retries up to the configured maximum (default 3) with backoff; operators can inspect pending/failed jobs via index-status.
- What happens when an agent attempts a skipped lifecycle step (e.g., OPEN directly to RESOLVED)? System rejects the transition.
- What happens when an agent transitions to RESOLVED with an empty or whitespace-only resolution field? System rejects the transition with a validation error.
- What happens when an agent attempts to edit or delete an existing comment? Not supported in v1; no edit/delete comment API or UI is provided.
- What happens when an agent attempts to delete a ticket? Not supported in v1; tickets are retained permanently; use CANCELLED status to mark abandoned work.

## Requirements *(mandatory)*

### Functional Requirements

**Ticket management**

- **FR-001**: System MUST allow users to create a support ticket with title, description, and priority; category is optional on create and defaults to GENERAL when omitted.
- **FR-002**: System MUST assign new tickets status OPEN by default.
- **FR-003**: System MUST display a paginated list of tickets showing identifier, title, status, and priority at minimum; pagination parameters (`page`, `size`, optional `sort`) are defined in the ticket API contract.
- **FR-004**: System MUST allow users to view full ticket detail including title, description, status, priority, assignee, category, and comment history.
- **FR-005**: System MUST allow users to update ticket title, description, priority, and assignee on existing tickets.
- **FR-006**: System MUST allow users to add comments to a ticket; each comment MUST record content, author, and timestamp.
- **FR-007**: System MUST persist all ticket data and comments so they remain available after application restart.
- **FR-008**: System MUST validate ticket input on the server and reject invalid submissions.
- **FR-009**: System MUST display meaningful validation and business-rule error messages in the user interface when an action fails. Field validation failures (HTTP 400, e.g., blank title, invalid question length) MUST map to the offending form field; state-machine rejections (HTTP 409, e.g., invalid transition) MUST display the problem `detail` in a banner without implying a field validation error.

**Search and filter**

- **FR-010**: System MUST allow users to search tickets by keyword matched against title and description.
- **FR-011**: System MUST allow users to filter the ticket list by status.

**Lifecycle state machine**

- **FR-012**: System MUST enforce ticket status transitions exclusively on the server; client-side state changes alone MUST NOT be authoritative.
- **FR-013**: System MUST allow only the valid transitions defined in User Story 3.
- **FR-014**: System MUST reject invalid status transitions with a clear error and leave ticket status unchanged.
- **FR-014a**: System MUST require a non-empty dedicated resolution field when transitioning a ticket to RESOLVED; transitions without resolution notes MUST be rejected.

**AI assistant (RAG)**

- **FR-015**: System MUST provide a natural-language question-answering capability over indexed ticket history.
- **FR-016**: System MUST index ticket content as separate searchable units: one for description, one per comment, and one for the resolution field (when present).
- **FR-017**: Each indexed unit MUST carry metadata: ticket identifier, content type (description, comment, or resolution), status, priority, assignee, and category; comment units MUST also carry comment identifier and timestamp.
- **FR-018**: System MUST re-embed indexed units when searchable text changes (description edit, comment add, resolution set or edit); metadata-only changes (status, priority, assignee, category) MUST update unit metadata without re-embedding unchanged text. Comment content is immutable after creation (no edit or delete in v1).
- **FR-019**: System MUST retrieve relevant ticket content before generating an answer (single retrieve-then-answer flow per question).
- **FR-020**: System MUST generate answers using only retrieved ticket context for support-specific questions; it MUST NOT supplement with general knowledge when ticket context is insufficient.
- **FR-021**: Every in-scope answer MUST include a structured `sources` array with one entry per cited ticket. Each entry MUST contain `ticketId` (internal UUID), `displayId` (human-readable, e.g. TKT-1001), and `contentTypes` (non-empty array of matched unit types: `description`, `comment`, and/or `resolution`). Multiple matched units from the same ticket MUST be aggregated into a single entry. The system MUST NOT return a generated in-scope answer when `sources` would be empty.
- **FR-022**: When no relevant tickets are found (including below-threshold retrieval, empty index, or missing citation metadata), the system MUST return the configured fixed no-match message in `answer`, an empty `sources` array, and MUST NOT fabricate ticket facts or plausible-sounding unsupported answers. The default message text is: *"I could not find any relevant tickets in our history that answer this question."* (overridable via `app.rag.no-match-message`).
- **FR-023**: The assistant MUST answer one question per request; it MUST NOT autonomously create tickets, send notifications, or chain into other actions.
- **FR-024**: Retrieval sensitivity parameters (result count limit and relevance threshold) MUST be configurable without changing application code. Defaults: `top-k=5`, similarity threshold `0.50` (local/Ollama profile may use `0.70` in quickstart).
- **FR-026**: When embedding or LLM dependencies are unavailable, the assistant API MUST return HTTP 503 (`application/problem+json`) and MUST NOT return a fabricated grounded answer.
- **FR-027**: Failed indexing jobs MUST retry with exponential backoff up to a configurable maximum (default 3 attempts); ticket mutations MUST remain committed regardless of indexing outcome.

**Quality & testability**

- **FR-025**: Before v1 is considered complete, automated tests MUST cover: (1) ticket state machine — all valid transitions succeed and invalid transitions (skipped steps, terminal reopen, RESOLVED without resolution, whitespace-only resolution) are rejected with clear errors; (2) RAG grounding — in-scope answers include structured `sources` with `ticketId`, `displayId`, and non-empty `contentTypes`, empty `sources` when metadata is missing, and no-match responses return empty `sources` with the fixed message without invoking the LLM; (3) core ticket API flows — create, list, detail, update, comment add, status transition, search, and filter.

### Key Entities

- **Ticket**: A support request. Key attributes: unique identifier (e.g., TKT-1001), title, description, status, priority, assignee, category, resolution (required once status is RESOLVED or later), created/updated timestamps. Has zero or more comments.
- **Comment**: A note attached to a ticket. Key attributes: content, author, timestamp. Belongs to one ticket.
- **Ticket Status**: Lifecycle state with values OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED. Governed by enforced transition rules.
- **Priority**: Severity level of a ticket (e.g., LOW, MEDIUM, HIGH). Used for filtering and retrieval metadata.
- **Assignee**: Person or team responsible for the ticket. May be unset.
- **Category**: Classification of the issue domain (e.g., payment, shipment, account). Used for organization and retrieval metadata.
- **Knowledge Document**: A single searchable unit derived from one ticket content section (description, one comment, or resolution field). Each unit is indexed independently and enriched with ticket metadata plus a content-type label for retrieval and citation.
- **Assistant Question**: A natural-language query submitted by a user seeking an answer from ticket history.
- **Assistant Answer**: A response containing `answer` text and a structured `sources` array (one entry per ticket: `ticketId` UUID + `displayId` + non-empty `contentTypes[]`), or an empty `sources` array with the configured fixed no-match message in `answer` when retrieval finds nothing relevant or citations cannot be resolved.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A support agent can create a ticket, find it in the list, and view its detail in under 2 minutes on first use.
- **SC-002**: 100% of invalid status transition attempts are rejected by the system with an understandable error message. Invalid cases include: any transition not in the User Story 3 table, OPEN/IN_PROGRESS → RESOLVED without resolution, RESOLVED/CLOSED/CANCELLED → OPEN or IN_PROGRESS, and any transition from terminal states (CLOSED, CANCELLED).
- **SC-003**: Ticket data—including comments and status changes—remains intact and retrievable after application restart in 100% of test scenarios.
- **SC-004**: Keyword search returns all tickets containing the search term in title or description with no false exclusions in test scenarios.
- **SC-005**: For in-scope questions with matching ticket data, 100% of assistant responses include a non-empty `sources` array where every entry contains `ticketId` (UUID), `displayId`, and a non-empty `contentTypes` array with allowed values only.
- **SC-006**: For questions with no relevant ticket data (including below-threshold retrieval and missing citation metadata), 100% of assistant responses contain the fixed no-match message in `answer`, an empty `sources` array, and no fabricated ticket facts; the LLM MUST NOT be invoked.
- **SC-007**: After a searchable-text change (description, comment, or resolution), a question about the new content returns an answer reflecting the update within one indexing cycle (default poll interval 2s, max job completion target 30s per plan); metadata-only changes do not require text re-embedding.
- **SC-008**: Support agents can complete the primary ticket workflow (create → update → comment → valid status transition) without encountering unexplained failures in 95% of guided test sessions.
- **SC-009**: Automated test suites for state machine, RAG grounding, and core ticket API flows pass in CI before v1 release.

## Assumptions

- Primary users are internal support agents managing customer issues; customer self-service portal is out of scope for this release.
- Assignee is stored as a display name or identifier string; full user directory and authentication are out of scope unless added later.
- Category uses a predefined set of values (PAYMENT, SHIPMENT, ACCOUNT, GENERAL); on ticket create, category is optional and defaults to GENERAL when not provided.
- Ticket identifiers follow a human-readable format such as TKT-1001.
- Resolution notes live in a dedicated ticket field, required on transition to RESOLVED; description, each comment, and resolution are indexed as separate knowledge documents.
- The assistant handles one question per interaction; multi-turn conversational memory is out of scope.
- Retrieval result count limit and relevance threshold have documented default values but remain operator-configurable (see FR-024 and plan Configuration).
- The fixed no-match assistant message is a named configuration value (`app.rag.no-match-message`) with the default documented in FR-022.
- v1 UI accessibility (WCAG compliance, i18n) is out of scope; basic semantic HTML and ARIA for loading/error states are implemented as best-effort only.
- Single-tenant deployment; multi-tenant isolation is out of scope.
- Role-based access control and audit logging beyond comment authorship are out of scope for v1.

## Out of Scope

- Comment edit and delete after creation (comments are add-only in v1).
- Ticket delete (tickets are permanent; CANCELLED is the abandonment path).
- Autonomous agent behavior (creating tickets, sending notifications, tool chaining) triggered by assistant responses.
- General-knowledge answers not grounded in ticket data.
- Customer-facing portal or external channel integrations (email, chat widgets).
- Full WCAG accessibility certification and internationalization (basic ARIA/semantic HTML only in v1).
- Authentication, authorization, and user management beyond assignee as a field.
- Engineering tooling setup (IDE rules, prompt history, AI review commands).
- Technology stack selection and implementation architecture (covered in planning phase).
