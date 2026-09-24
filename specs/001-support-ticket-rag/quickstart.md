# Quickstart: Validate AI-Powered Support Ticket Management

**Feature**: `001-support-ticket-rag` | **Date**: 2025-09-25

Runnable validation scenarios. Implementation details live in `tasks.md`.

## Prerequisites

- Java 21, Maven 3.9+
- Node.js 20+, npm
- Docker (Testcontainers + local PostgreSQL)
- **Ollama** (local RAG — no API key required; see below)
- OpenAI API key — **only** if using `SPRING_PROFILES_ACTIVE=openai` (staging/production)

## Install Ollama (local development)

```bash
# macOS
brew install ollama
brew services start ollama

# Pull models used by application-local.yml
ollama pull nomic-embed-text   # embeddings (768 dimensions)
ollama pull llama3.2:3b        # chat (~2 GB)
```

Verify:

```bash
curl -s http://localhost:11434/api/tags | jq '.models[].name'
```

## Environment

Copy `.env.example` to `.env` at the repo root (or export variables in your shell):

```bash
# Local profile (default) — Ollama, no API key
export SPRING_PROFILES_ACTIVE=local
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=ticketing
export DB_USER=ticketing
export DB_PASSWORD=ticketing
export OLLAMA_BASE_URL=http://localhost:11434
export EMBEDDING_DIMENSIONS=768
export APP_RAG_RETRIEVAL_TOP_K=5
export APP_RAG_RETRIEVAL_SIMILARITY_THRESHOLD=0.70
```

For OpenAI (staging/production):

```bash
export SPRING_PROFILES_ACTIVE=openai
export OPENAI_API_KEY=sk-...
export EMBEDDING_DIMENSIONS=1536
export APP_RAG_RETRIEVAL_SIMILARITY_THRESHOLD=0.75
```

## Start Infrastructure

```bash
docker compose up -d
```

## Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

Liquibase migrations apply on startup. Verify health:

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

## Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

---

## Scenario 1: Ticket CRUD (User Story 1)

```bash
# Create ticket
curl -s -X POST http://localhost:8080/api/v1/tickets \
  -H 'Content-Type: application/json' \
  -d '{"title":"Payment timeout","description":"Customer card declined on checkout","priority":"HIGH","category":"PAYMENT"}' \
  | jq .

# List tickets
curl -s 'http://localhost:8080/api/v1/tickets' | jq '.items[] | {displayId, title, status}'

# Get detail (replace {id} with returned uuid)
curl -s http://localhost:8080/api/v1/tickets/{id} | jq .
```

**Expected**: Status `OPEN`, `displayId` like `TKT-1001`, fields match request.

---

## Scenario 2: Comments & Updates (User Story 2)

```bash
# Add comment
curl -s -X POST http://localhost:8080/api/v1/tickets/{id}/comments \
  -H 'Content-Type: application/json' \
  -d '{"content":"Retried with backup gateway","author":"agent@support"}' | jq .

# Update title
curl -s -X PATCH http://localhost:8080/api/v1/tickets/{id} \
  -H 'Content-Type: application/json' \
  -d '{"title":"Payment timeout - resolved via backup gateway"}' | jq .

# Invalid update (empty title)
curl -s -X PATCH http://localhost:8080/api/v1/tickets/{id} \
  -H 'Content-Type: application/json' \
  -d '{"title":"   "}' | jq .
```

**Expected**: Comment persists with author/timestamp. Valid update saves. Empty title returns 400 problem+json.

---

## Scenario 3: State Machine (User Story 3)

```bash
# OPEN -> IN_PROGRESS
curl -s -X PATCH http://localhost:8080/api/v1/tickets/{id}/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}' | jq .status

# IN_PROGRESS -> RESOLVED (with resolution)
curl -s -X PATCH http://localhost:8080/api/v1/tickets/{id}/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"RESOLVED","resolution":"Switched to backup payment gateway"}' | jq .status

# RESOLVED -> CLOSED
curl -s -X PATCH http://localhost:8080/api/v1/tickets/{id}/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"CLOSED"}' | jq .status

# Invalid: CLOSED -> OPEN (expect 409)
curl -s -X PATCH http://localhost:8080/api/v1/tickets/{id}/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"OPEN"}' | jq .
```

**Expected**: Valid transitions succeed. Terminal reopen returns 409. Missing resolution on RESOLVED returns 400.

---

## Scenario 4: Search & Filter (User Story 4)

```bash
curl -s 'http://localhost:8080/api/v1/tickets?q=payment' | jq '.total'
curl -s 'http://localhost:8080/api/v1/tickets?status=CLOSED' | jq '.items[].status' | uniq
curl -s 'http://localhost:8080/api/v1/tickets?q=nonexistent-term-xyz' | jq '.total'
```

**Expected**: Keyword matches title/description. Status filter returns only matching status. No matches returns `total: 0`, not error.

---

## Scenario 5: RAG Q&A (User Story 5)

Wait for indexing job to complete (or poll index status):

```bash
curl -s http://localhost:8080/api/v1/assistant/index-status/{id} | jq .
```

Ask grounded question:

```bash
curl -s -X POST http://localhost:8080/api/v1/assistant/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"Have we seen payment failures before?"}' | jq .
```

**Expected**: Non-empty `sources` with `ticketId` + `contentType`. Answer references payment content.

Ask no-match question:

```bash
curl -s -X POST http://localhost:8080/api/v1/assistant/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What is the weather in Tokyo?"}' | jq .
```

**Expected**: Fixed no-match message in `answer`, `sources: []`.

Re-index validation — add comment, wait for index, re-ask:

```bash
curl -s -X POST http://localhost:8080/api/v1/tickets/{id}/comments \
  -H 'Content-Type: application/json' \
  -d '{"content":"Root cause was expired API key on Stripe","author":"agent@support"}'

# wait ~5s for indexing job
curl -s -X POST http://localhost:8080/api/v1/assistant/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What was the root cause of the payment failure?"}' | jq .
```

**Expected**: Answer reflects new comment content.

---

## Scenario 6: Integration Test Suite

```bash
cd backend
./mvnw verify -Pintegration-tests
```

**Expected**: All state-machine and RAG retrieval integration tests pass against Testcontainers PGVector.

## Scenario 7: Frontend Smoke

1. Open ticket list — see created tickets.
2. Create ticket via form — appears in list.
3. Open detail — comments, status actions visible.
4. Transition status — UI shows error on invalid transition.
5. Open Assistant page — ask question — sources render as links to tickets.

---

## Contract References

- Ticket API: `contracts/tickets-api.yaml`
- RAG API: `contracts/rag-api.yaml`
- State machine: `contracts/state-machine.md`
- Data model: `data-model.md`
