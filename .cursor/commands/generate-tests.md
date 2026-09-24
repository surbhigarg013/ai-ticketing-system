# Generate Tests

Generate tests for the **selected or current code** using the repository's existing testing frameworks, patterns, fixtures, and utilities. **Modify test files only** unless a minimal test helper is already established in the repo.

## Before you start

1. Inspect build files and test directories: e.g. `src/test/java`, `**/*.test.ts`, `**/*.spec.tsx`, `vitest.config.*`, `jest.config.*`, `pom.xml` / `build.gradle.kts` test deps.
2. Mirror naming, package layout, annotations, and assertion libraries already in use (JUnit 5, Mockito, `@WebMvcTest`, Vitest, React Testing Library, etc.).
3. Follow `.cursor/rules/testing.mdc` for coverage targets and what to cover; do not add new test dependencies.

## Scope

- Target the selected symbol, class, component, or file(s) in focus.
- If scope is unclear, ask once, then test the smallest unit that holds the behavior.

## What to cover

- Happy paths and primary error paths
- Validation failures (Bean Validation and domain rules)
- State machine transitions (allowed and rejected) when present
- RAG behavior when present: cited ticket IDs, no-hit response, retrieval-only grounding
- API contracts: status codes and problem+json shape for controllers
- Frontend: validation messages, disabled submit, error display on failed API calls
- Major conditional branches in the target code

## Constraints

- Test **behavior and outcomes**, not private methods or implementation details.
- Reuse existing fixtures, `@MockBean`, test containers, MSW handlers, and factory helpers.
- Exclude POJO-only types from coverage goals per `testing.mdc`.
- No new libraries; no snapshot tests unless the repo already uses them.

## Workflow

1. Read production code and existing tests in the same module.
2. Add or extend tests in the conventional location (`*Test.java`, `*.test.tsx`, etc.).
3. Run the narrowest test command that exists in the repo, for example:
   - `./mvnw test -Dtest=ClassName`
   - `./gradlew test --tests ClassName`
   - `npm test -- path/to/file.test.ts`
4. Fix failing tests you introduced; do not change production code unless a test exposes a clear bug—then report the bug and stop short of fixing prod unless asked.

## Output

After running tests, report:

```text
## Tests generated
- **Target**: path/to/source
- **Files added/updated**: path/to/test files
- **Scenarios**: bullet list of behaviors covered

## Run result
- **Command**: ...
- **Status**: pass | fail
- **Notes**: failures, flakiness, or Unable to run (missing toolchain)
```

If the repo has no test harness yet, scaffold tests in the conventional layout for Spring Boot + React/TypeScript and state what build file changes would be needed—do not edit build files unless the user asks.
