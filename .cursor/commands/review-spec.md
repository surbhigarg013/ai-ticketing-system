# Review Spec

Compare a **provided specification or requirements** against the **actual implementation**. Trace each requirement to backend code, frontend code, and tests. **Do not modify code.**

## Before you start

1. Inspect the repository: build files, source roots, test layout, and any feature artifacts under `.specify/` (`spec.md`, `plan.md`, `tasks.md`).
2. Use only frameworks and patterns present in the repo; do not assume tools that are not configured.
3. Treat `.cursor/rules/*.mdc` and `.specify/memory/constitution.md` as non-functional requirements (RAG grounding, validation ownership, coverage expectations, API shape).

## Inputs

- **Spec**: user-pasted requirements, attached `spec.md`, or explicit acceptance criteria. If missing, ask once.
- **Implementation**: search the codebase for controllers, services, components, API clients, migrations, and config that satisfy each requirement.

## Trace method

For each requirement (numbered or bulleted):

1. Quote or paraphrase the requirement in one line.
2. Locate implementation evidence (file paths and symbols).
3. Locate test evidence (test class/file and scenario name), if any.
4. Assign status:

| Status | Meaning |
|--------|---------|
| **Implemented** | Behavior matches spec; tests cover the requirement when tests exist in repo |
| **Partial** | Some behavior present; gaps in logic, UX, errors, or tests |
| **Missing** | No implementation found |
| **Incorrect** | Code exists but contradicts the spec |
| **Unable to Verify** | Cannot confirm (missing spec detail, env dependency, or no runnable tests) |

## Domain checks (when applicable)

- Ticket state machine: allowed vs rejected transitions match spec.
- RAG: retrieved-only answers, source ticket IDs, no-hit response, re-index on mutation, externalized retrieval params.
- API: REST shape, status codes, problem+json errors per `api-standards.mdc`.
- Frontend: validation and error states per spec.

## Output

```text
## Spec review summary
- Total requirements: N
- Implemented: X | Partial: Y | Missing: Z | Incorrect: W | Unable to Verify: U

## Requirement trace
### REQ-1: <one-line requirement>
- **Status**: Implemented | Partial | Missing | Incorrect | Unable to Verify
- **Backend**: path:symbol (or —)
- **Frontend**: path:symbol (or —)
- **Tests**: path:testName (or —)
- **Gap / notes**: ...
- **Recommended action**: ...

## Priority gaps
1. ...
```

Keep file references exact. Prefer evidence over inference; mark **Unable to Verify** when evidence is insufficient.
