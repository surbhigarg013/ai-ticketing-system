# Review Code

Review the **selected or current code** for correctness, bugs, security, performance, maintainability, error handling, and violations of project conventions. **Do not modify code.**

## Before you start

1. Inspect the repository: build files (`pom.xml`, `build.gradle.kts`, `package.json`), source roots, test layout, and CI config.
2. Use only frameworks and patterns present in the repo; do not assume tools that are not configured.
3. Treat `.cursor/rules/*.mdc` and `.specify/memory/constitution.md` as the convention baseline (cite the rule when flagging a violation).

## Scope

Review what the user selected or the file(s) in focus. If scope is unclear, ask once, then proceed with the narrowest reasonable set.

### Spring Boot / Java (when present)

- Layering: thin controllers, business logic and `@Transactional` in services, persistence in repositories.
- Constructor injection; records for immutable DTOs; Bean Validation on inputs; domain rules in services.
- RFC 9457 `ProblemDetail` errors; correct HTTP status codes.
- Ticket state transitions, RAG grounding, and indexing rules per `rag-vector-store.mdc`.
- Security: injection, auth gaps, secrets in code/logs, unsafe deserialization, missing validation.
- Performance: N+1 queries, unbounded fetches, missing pagination, blocking calls in hot paths.

### React / TypeScript (when present)

- Input validation and error/loading/empty states on user-facing flows.
- Accessible, testable components; avoid leaking API types into UI without mapping.
- Effect cleanup, stale closures, unnecessary re-renders, missing error boundaries where appropriate.

## Output

Report **only actionable findings**. Skip praise, style nitpicks without impact, and issues outside scope.

Use this structure per finding:

```text
### [CRITICAL|HIGH|MEDIUM|LOW] Short title
- **File**: path/to/file.ext:line
- **Issue**: what is wrong
- **Impact**: what breaks or risks
- **Fix**: concrete change (code snippet optional, keep minimal)
```

End with a short summary: counts by severity and the top 1–3 fixes to do first.

If no issues: say so in one line and note any residual risks or untested paths worth a follow-up.
