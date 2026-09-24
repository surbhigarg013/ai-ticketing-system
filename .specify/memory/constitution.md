<!--
Sync Impact Report
==================
Version change: (unratified template) → 1.0.0
Modified principles: N/A (initial ratification)
Added sections:
  - Core Principles (5 principles)
  - Technology Stack Requirements
  - Development Workflow & Quality Gates
  - Governance
Removed sections: None
Follow-up TODOs:
  - None
-->

# AI Ticketing System Constitution

## Core Principles

### I. Backend Architecture & Modularity

The backend MUST target Java 21 with idiomatic Spring Boot and Spring AI patterns.

- Code MUST follow a clean, modular architecture aligned with SOLID principles.
- Dependencies MUST be injected via constructors; field injection is prohibited.
- MapStruct MUST be used for class-to-class mapping.
- Lombok MAY be used for getters, setters, and other boilerplate reduction.
- Liquibase MUST manage all database schema changes.

**Rationale**: A consistent backend architecture reduces coupling, improves testability,
and keeps mapping and schema evolution predictable across features.

### II. Persistence & Frontend Stack

- PostgreSQL MUST be the system of record for relational data.
- PGVector MUST power vector search and retrieval for ticket knowledge.
- The frontend MUST be built with React and TypeScript.

**Rationale**: A single persistence platform with native vector support simplifies RAG
pipelines; React + TypeScript provides type-safe, maintainable UI development.

### III. Validation, State Ownership & Testing

- The backend MUST own authoritative validation and all ticket state transitions.
- The frontend MUST implement input validation and explicit error-state handling for
  user-facing forms and interactions.
- Automated tests MUST cover business rules and the ticket state machine, including
  valid transitions, invalid transitions, and edge cases.

**Rationale**: Splitting validation responsibilities correctly prevents inconsistent state;
tests on business rules and state machines protect core domain integrity.

### IV. RAG Integrity (NON-NEGOTIABLE)

Retrieval-augmented answers MUST be grounded strictly in retrieved ticket context.

- RAG responses MUST use only context retrieved from indexed ticket knowledge; no
  hallucinated or unstated ticket facts.
- Every RAG response MUST cite the ticket ID(s) that supported the answer.
- When retrieval returns no relevant tickets, the system MUST explicitly report that no
  relevant tickets were found—never fabricate an answer.
- `top-k` and similarity threshold MUST be configurable without code changes.
- Ticket knowledge MUST be re-indexed after mutations that affect searchable content
  (create, update, delete, or status changes that alter indexed fields).

**Rationale**: Trustworthy AI assistance depends on traceable, retrieval-bound answers
and fresh indexes after data changes.

### V. Security, Documentation & Specification-First Delivery

- Secrets MUST NEVER be committed to version control; use environment variables or a
  secure secret manager.
- When resolving framework or API questions, official documentation MUST be preferred
  over third-party summaries or outdated examples.
- Implementation MUST NOT begin until the feature specification, implementation plan,
  and task list have been reviewed and approved.

**Rationale**: Preventing secret leakage and grounding decisions in authoritative docs
reduces risk; specification-first delivery avoids rework and scope drift.

## Technology Stack Requirements

| Layer | Requirement |
|-------|-------------|
| Runtime | Java 21 |
| Backend framework | Spring Boot, Spring AI (idiomatic usage) |
| Mapping | MapStruct |
| Boilerplate | Lombok (getters/setters and related) |
| Schema migrations | Liquibase |
| Database | PostgreSQL |
| Vector search | PGVector |
| Frontend | React + TypeScript |

New dependencies MUST be justified, pinned, and documented in the implementation plan
before adoption.

## Development Workflow & Quality Gates

1. **Specify** — Capture requirements in `spec.md` and resolve ambiguities before planning.
2. **Plan** — Produce `plan.md` with architecture, data model, and integration decisions.
3. **Tasks** — Break work into ordered, testable tasks in `tasks.md`.
4. **Review** — Obtain explicit review/approval of spec, plan, and tasks before coding.
5. **Implement** — Follow constitution principles; keep changes minimal and reviewable.
6. **Validate** — Run tests for business rules and state machine changes; verify RAG
   citation and no-context behavior.

Pull requests MUST demonstrate compliance with Core Principles, especially RAG integrity
and backend ownership of ticket state transitions.

## Governance

This constitution supersedes ad-hoc conventions for the AI Ticketing System project.

**Amendment procedure**

1. Propose changes with rationale and impact on existing specs/plans.
2. Update this document with a Sync Impact Report comment during review.
3. Bump `CONSTITUTION_VERSION` per semantic versioning:
   - **MAJOR** — Backward-incompatible principle removals or redefinitions.
   - **MINOR** — New principles or materially expanded guidance.
   - **PATCH** — Clarifications, wording, or non-semantic refinements.
4. Set `LAST_AMENDED_DATE` to the amendment date; preserve `RATIFICATION_DATE`.
5. Remove the Sync Impact Report comment before committing the amended constitution.

**Compliance**

- All feature work MUST be checked against this constitution during spec, plan, and PR review.
- Violations MUST be justified in writing or corrected before merge.
- Runtime development guidance lives in Spec Kit artifacts (`spec.md`, `plan.md`, `tasks.md`)
  and project rules under `.cursor/rules/`.

**Version**: 1.0.0 | **Ratified**: 2025-09-25 | **Last Amended**: 2025-09-25
