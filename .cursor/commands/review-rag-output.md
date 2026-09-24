# Review RAG Output

Audit a **RAG answer** against its **retrieval payload** for grounding violations. **Do not modify code.**

## Before you start

1. Inspect `.cursor/rules/rag-vector-store.mdc` and `.specify/memory/constitution.md` (Principle IV) for expected behavior.
2. Require inputs from the user (or attached logs/responses). If any input is missing, ask once:
   - **User query**
   - **Retrieval results** (chunks/documents with `ticketId`, score, and text)
   - **Model answer** (final text shown to user)
   - **Declared `sourceTicketIds`** and **`retrievalEmpty`** flag (if present)
   - **Expected no-answer message** (from spec, API contract, or `rag-vector-store.mdc`)

## Verification checklist

For each item, record **PASS**, **FAIL**, or **UNABLE TO VERIFY** with evidence.

| # | Check |
|---|-------|
| 1 | Every **factual support claim** in the answer is supported by text in retrieved ticket context |
| 2 | Every **cited ticket ID** appears in the retrieval results |
| 3 | No **external or general LLM knowledge** fills gaps (no facts absent from retrieval) |
| 4 | **Empty retrieval** (`retrievalEmpty: true` or no hits above threshold) produces the specified no-answer response only — no fabricated ticket facts |
| 5 | **Unsupported claims** are listed explicitly as **hallucinations** |

## How to judge claims

- Split the answer into atomic factual statements (status, dates, assignee, resolution, counts, causal claims about tickets).
- Map each statement to retrieval chunk text and ticket ID. Paraphrase OK only if meaning is fully entailed by retrieved text.
- Citation without support in that ticket's retrieved text = **hallucination**.
- Generic phrasing ("based on the tickets…") with unsupported specifics still fails check 1.
- If retrieval is empty but answer contains ticket-specific facts, fail checks 3 and 4.

## Output

```text
## RAG grounding review
- **Verdict**: GROUNDED | PARTIALLY GROUNDED | UNGROUNDED | NO-ANSWER OK | UNABLE TO VERIFY

## Checklist
1. Factual claims supported: PASS | FAIL | UNABLE TO VERIFY
2. Cited IDs in retrieval: PASS | FAIL | UNABLE TO VERIFY
3. No external LLM knowledge: PASS | FAIL | UNABLE TO VERIFY
4. No-match response correct: PASS | FAIL | N/A | UNABLE TO VERIFY
5. Hallucinations reported: PASS | FAIL (list below)

## Claim trace
| Claim | Supported? | Ticket ID | Retrieval evidence (quote or —) |
|-------|------------|-----------|----------------------------------|
| ...   | yes/no     | ...       | ...                              |

## Hallucinations
- **Claim**: ...
  **Why unsupported**: ...
  **Cited ID (if any)**: ...

## Citation issues
- IDs in answer but not in retrieval: [...]
- IDs in retrieval but never cited (informational only): [...]

## Recommended action
- ...
```

Report only actionable findings. No code changes unless the user asks for a fix.
