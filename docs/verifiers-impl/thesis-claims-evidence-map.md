# Thesis claims and evidence map

Use this page to move from a thesis claim draft to supporting verifier documentation, validation evidence, and limitations. It is an entry point for writing and claim-checking, not a replacement for the source pages.

Before citing any claim, verify the current baseline in [`current-state.md`](current-state.md) and terminology in [`glossary.md`](glossary.md).

## Claim map

| Claim draft | Supporting docs | Evidence to cite | Do not overclaim |
|---|---|---|---|
| The verifier statically extracts saga-oriented scenario structure from simulator applications and tests. | [`current-state.md`](current-state.md), [`roadmap.md`](roadmap.md), [`verifier-pipeline-plain-explanation.md`](verifier-pipeline-plain-explanation.md), [`reference/source-to-input-flow.md`](reference/source-to-input-flow.md) | Implemented current-state sections for application/source discovery, static production-code extraction, Groovy test/input extraction, and scenario catalog generation. | Do not claim arbitrary execution, fault injection, or domain-impact scoring exists in the current baseline. |
| The scenario catalog is deterministic and intended as the handoff contract for later execution/search stages. | [`current-state.md`](current-state.md), [`roadmap.md`](roadmap.md), [`decisions/2026-04-27-scenario-catalog-export-contract.md`](decisions/2026-04-27-scenario-catalog-export-contract.md) | Catalog artifacts listed in current state: `scenario-catalog.jsonl`, manifest, rejected inputs, and bounded generation behavior. | Do not claim the catalog entries are already executable by a runtime runner. |
| The verifier separates static scenario structure from optional runtime evidence. | [`current-state.md`](current-state.md), [`reference/dynamic-enrichment.md`](reference/dynamic-enrichment.md), [`archive/investigations/dynamic-enrichment-joining-reference.md`](archive/investigations/dynamic-enrichment-joining-reference.md), [`decisions/2026-04-28-hybrid-static-dynamic-key-binding.md`](decisions/2026-04-28-hybrid-static-dynamic-key-binding.md) | Dynamic enrichment writes sidecar artifacts (`scenario-catalog-enriched.jsonl`, enriched manifest, join report) while leaving the original static catalog unchanged. | Do not describe dynamic evidence as redefining static scenario structure. It is additive sidecar evidence. |
| Runtime input attribution improves exact static/dynamic joining for the current comparable Quizzes sagas-local baseline. | [`current-state.md`](current-state.md), [`reference/dynamic-enrichment.md`](reference/dynamic-enrichment.md), [`archive/investigations/dynamic-enrichment-joining-reference.md`](archive/investigations/dynamic-enrichment-joining-reference.md), [`decisions/2026-05-12-runtime-input-variant-attribution.md`](decisions/2026-05-12-runtime-input-variant-attribution.md), [`archive/implementation-log/2026-W20.md`](archive/implementation-log/2026-W20.md) | Refreshed counts moved from `MATCHED_EXACT=0`, `MATCHED_HIGH_CONFIDENCE=2`, `AMBIGUOUS=44`, `UNMATCHED=20`, `warningCount=8238` to `MATCHED_EXACT=46`, `MATCHED_HIGH_CONFIDENCE=0`, `AMBIGUOUS=3`, `UNMATCHED=17`, `warningCount=328`. | Do not generalize this result to stream/gRPC/distributed execution, causal/TCC runtime hooks, or arbitrary applications without additional validation. |
| The current dynamic-enrichment join is intentionally conservative. | [`reference/dynamic-enrichment.md`](reference/dynamic-enrichment.md), [`archive/investigations/dynamic-enrichment-joining-reference.md`](archive/investigations/dynamic-enrichment-joining-reference.md), [`glossary.md`](glossary.md) | Join statuses distinguish `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `MATCHED_PARTIAL`, `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED`; ambiguous candidates are not guessed. | Do not hide ambiguous/unmatched/not-covered outcomes. They are part of the honest evidence boundary. |
| The verifier work currently targets saga-catalog generation and dynamic-evidence enrichment, with only a narrow ScenarioExecutor POC beyond that baseline. | [`current-state.md`](current-state.md), [`roadmap.md`](roadmap.md), [`reference/scenario-executor-poc.md`](reference/scenario-executor-poc.md), [`archive/meeting-notes/index.md`](archive/meeting-notes/index.md) | Current-state boundaries list static extraction, bounded scenario catalog generation, dynamic enrichment, and a supported single-saga executor POC; roadmap still places generic execution, fault injection, impact analysis, GA search, and prioritization after the current baseline. | Do not present the executor POC as generic scenario execution or fault injection. |
| Historical docs provide traceability for engineering and thesis framing, but current claims must be checked against current-state docs. | [`reading-guide.md`](reading-guide.md), [`archive/implementation-log/index.md`](archive/implementation-log/index.md), [`archive/meeting-notes/index.md`](archive/meeting-notes/index.md), [`archive/index.md`](archive/index.md) | Landing pages label implementation logs as engineering history, meeting notes as advisor/thesis history, and archive material as provenance. | Do not cite historical notes as current truth without cross-checking current-state, roadmap, and later decisions. |

## Evidence workflow for writing

1. Start with [`current-state.md`](current-state.md) to confirm the present-tense implementation status.
2. Use [`glossary.md`](glossary.md) to stabilize terms such as `ScenarioPlan`, input variant, source mode, sidecar artifact, and join status.
3. Use [`reference/dynamic-enrichment.md`](reference/dynamic-enrichment.md) for a short thesis-friendly explanation of the dynamic-enrichment bridge.
4. Use [`archive/investigations/dynamic-enrichment-joining-reference.md`](archive/investigations/dynamic-enrichment-joining-reference.md) only when you need implementation traceability, join algorithm detail, ambiguity patterns, sidecar contents, or code references.
5. Use [`decisions/`](decisions/index.md) for durable rationale behind design choices.
6. Use [`archive/implementation-log/`](archive/implementation-log/index.md) for week-specific validation and progress history.
7. Use [`archive/meeting-notes/`](archive/meeting-notes/index.md) for advisor-facing framing and thesis discussion history.
8. Use [`archive/`](archive/index.md) only for provenance after checking current sources.

## Claim safety checklist

- Does the claim say whether it is current implementation, roadmap intent, or historical framing?
- Does the claim have at least one present-tense source in [`current-state.md`](current-state.md) or a current deep dive?
- Does the claim include the relevant limitation or rejected scope?
- If the claim uses dynamic-enrichment counts, does it name the comparable Quizzes sagas-local baseline rather than implying universal coverage?
- If the claim mentions execution, does it distinguish the narrow ScenarioExecutor POC from generic execution, fault injection, impact scoring, GA search, or bandit prioritization?
