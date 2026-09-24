# AI-Powered Support Ticket Management

Support ticket system with lifecycle enforcement, search/filter, and a RAG-based knowledge assistant over indexed ticket history.

## Prerequisites

- Java 21, Maven 3.9+
- Node.js 20+, npm
- Docker (PostgreSQL with PGVector)
- **Ollama** for local development (embeddings + chat; no API key required)

See [specs/001-support-ticket-rag/quickstart.md](specs/001-support-ticket-rag/quickstart.md) for full validation scenarios.

## Quick Start

### 1. Environment

Copy `.env.example` to `.env` (or export variables in your shell):

```bash
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

Install Ollama models:

```bash
ollama pull nomic-embed-text
ollama pull llama3.2:3b
```

### 2. Database

```bash
docker compose up -d
```

### 3. Backend

```bash
cd backend
./mvnw spring-boot:run
```

Verify health: `curl -s http://localhost:8080/actuator/health`

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

## Features

| Area | Description |
|------|-------------|
| Tickets | Create, list, detail, update, comments |
| Lifecycle | Server-enforced state machine (OPEN → … → CLOSED) |
| Search | Keyword search on title/description + status filter |
| Assistant | RAG Q&A with grounded answers and source citations |

## Project Structure

```
backend/          Spring Boot API (Java 21)
frontend/         Vite + React 19 + TypeScript
specs/            Feature specs, contracts, quickstart
docker-compose.yml  PostgreSQL + PGVector
```

## OpenAI Profile (optional)

For cloud embeddings/chat instead of Ollama:

```bash
export SPRING_PROFILES_ACTIVE=openai
export OPENAI_API_KEY=sk-...
export EMBEDDING_DIMENSIONS=1536
export APP_RAG_RETRIEVAL_SIMILARITY_THRESHOLD=0.75
```

## API Documentation

With the backend running, OpenAPI docs are available at `/swagger-ui.html`.

## License

See repository license file if present.
