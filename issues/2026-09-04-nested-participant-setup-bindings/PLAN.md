# Plan: nested participant setup bindings

## Environment and strategy

Authorized follow-up after the completed event-receiver issue (2026-09-05). Use Sol Medium for the bounded
mapper/test implementation and Astra for occurrence/target-boundary review and integration.
Start from the current 577 package; preserve all existing dirty changes. The existing Sol task implements in the primary checkout with exclusive ownership of the
source/test slice; Astra works read-only on impact analysis and reviews the incremental
diff against the saved starting patch. No commits, pushes or merges are authorized.

## M0 — Audit exact source tuples

Pin the three CreateQuiz inputs and an ordinary CreateQuestion representative (for example
CancelTournamentTest#createQuestion called from setup). Trace courseExecution, returned
Question/Topic DTO and containing collection to exact prior setup occurrences. Inspect
whether the chosen setup repeats the measured target. Record cases where old fixture-only
priority must not authorize unsafe replay. Do not assume all 85 share a safe setup prefix.

## M1 — Reuse recursive value binding

Extend participant argument selection only when its typed recipe contains an exact retained
source reference. Reuse mapValue for the whole DTO rather than substituting one nested field.
Preserve unresolved dependencies and existing exact property/method/type validation. Cover
nested DTO/list/set, several earlier producers, literal fields/mutations, missing/later
producer, wrong type, selected-target replay and no-reference regression in dummyapp tests.

## M2 — Measure and qualify

Run focused tests, ordinary single generation and compare base accounting and gained/lost
input IDs. Independently review source identity, target cutoff and negative coverage.
Run the full verifier suite once integrated; repeat generation for byte stability. Use
bounded fresh Docker attempts for the three newly recovered quiz inputs and a representative
question input only if safely admitted. Report static improvement separately from runtime
success. A projected 580 or665 is not evidence and must not become the acceptance oracle.

## M3 — Handoff

Update canonical docs with exact shipped coverage, surviving blockers, IDs, source contexts,
commands and results. If the85-input cohort needs new helper semantics, leave its package
separate from the small repair and explain the remaining dependency precisely.

## Risks and proof boundaries

The largest risk is successful reconstruction with the wrong source occurrence or with
already-executed target effects. A higher count alone is insufficient proof. Shared course,
question and topic identity, target exclusion and deterministic negative outcomes matter.
