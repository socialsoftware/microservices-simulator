# Verifier docs reading guide

Use this guide to choose the shortest useful path through the verifier knowledge base.

If terminology is the blocker, keep [`glossary.md`](glossary.md) open while reading. It defines recurring scenario, input, source-mode, and dynamic-enrichment terms.

If you are drafting thesis prose, use [`thesis-claims-evidence-map.md`](thesis-claims-evidence-map.md) to connect claims to support docs, validation evidence, and limitations.

## If you need thesis evidence

Start with [`thesis-claims-evidence-map.md`](thesis-claims-evidence-map.md). It maps claim drafts to current-truth docs, deep dives, decisions, validation logs, historical framing, and explicit non-overclaim guidance.

Then follow the map back to [`current-state.md`](current-state.md), [`dynamic-enrichment-joining-explained.md`](dynamic-enrichment-joining-explained.md), [`dynamic-enrichment-joining-reference.md`](dynamic-enrichment-joining-reference.md), [`implementation-log/`](implementation-log/index.md), and [`meeting-notes/`](meeting-notes/index.md) as needed.

## If you need the current truth

Start with [`current-state.md`](current-state.md). It is the present-tense source for implemented behavior, partial behavior, known limits, validation baseline, and next priorities.

Then read [`roadmap.md`](roadmap.md) to see how the current baseline fits the intended verifier trajectory.

## If you need the narrative overview

Read [`verifier-pipeline-plain-explanation.md`](verifier-pipeline-plain-explanation.md) for the high-level verifier flow from source analysis through scenario generation and enrichment.

Use [`test-analysis-saga-input-flow.md`](test-analysis-saga-input-flow.md) when the question is specifically about how tests become saga input variants.

Use [`dynamic-enrichment-joining-explained.md`](dynamic-enrichment-joining-explained.md) first when the question is how static scenarios are matched with runtime evidence.

Use [`dynamic-enrichment-joining-reference.md`](dynamic-enrichment-joining-reference.md) when you need the full static pipeline, runtime attribution flow, join algorithm, sidecar artifacts, ambiguity causes, and code references.

Use [`glossary.md`](glossary.md) for quick definitions of `ScenarioPlan`, input variant, dynamic input map, and join statuses before reading the dynamic-enrichment material.

## If you need durable rationale

Use [`decisions/`](decisions/index.md) for architectural choices and rejected alternatives. These explain why the verifier took a specific direction, but they are not a replacement for current implementation status.

## If you need recent engineering history

Use [`implementation-log/`](implementation-log/index.md) to answer what changed in a specific week, what was validated, and what remained deferred.

For the latest engineering state, still return to [`current-state.md`](current-state.md) after reading logs.

## If you need advisor or thesis history

Use [`meeting-notes/`](meeting-notes/index.md) for advisor-facing framing, thesis discussion, and historical claims made at specific checkpoints.

Meeting notes are evidence of how the work was framed at a point in time. They are not the canonical source for what is true now.

## If you need provenance or old working material

Use [`archive/`](archive/index.md) for archived material and older working notes.

Use [`scenario-catalog-followup-prompt.md`](scenario-catalog-followup-prompt.md) only as handoff context. It remains reachable for provenance but is intentionally not part of the main reading path.
