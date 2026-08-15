# Keep static package identity separate from dynamic evidence

Date: 2026-04-28

Status: active; updated for the v4 package boundary on 2026-08-01

## Context

Static analysis can extract Saga structure, ordered steps, test-derived inputs, and aggregate-type footprints deterministically, but it cannot always prove the concrete runtime identity behind a value. Exact propagation may cross Groovy fixtures, helpers, facades, Saga fields, commands, handlers, services, and runtime-generated ids.

Pure static propagation across every supported Java/Groovy shape would be a large interprocedural value-analysis project. Replacing static generation with runtime traces would make package identity depend on which tests happened to run and would lose deterministic coverage for unexecuted paths.

## Decision

Keep a hybrid pipeline with a hard ownership boundary:

1. Static analysis owns InputVariants, WorkloadPlans, FaultScenarios, vectors, action schedules, and the five-file v4 package identity.
2. Optional dynamic enrichment runs selected tests with generic simulator evidence hooks.
3. The verifier joins runtime evidence back to static inputs and WorkloadPlans conservatively.
4. Dynamic outputs remain workload-linked sidecars. They may strengthen attribution and explain uncertainty, but they do not create or rewrite semantic package records.

Exact attribution requires raw runtime evidence to carry a verifier-generated `inputVariantId`. Test/functionality/step shape without a direct id may support high confidence, but not exactness. Ambiguous evidence is retained as ambiguous rather than guessed.

The verifier writes a run-level `dynamic-input-map.json` from accepted static inputs. Runtime attribution uses test ownership, actual functionality class identity, and step identity. Provenance continues to explain where an input was found; ownership controls which running feature may claim it.

## Why this boundary

- Static generation remains deterministic, bounded, and available without executing tests.
- Runtime evidence can observe fixture-created and generated values that static analysis only represents symbolically.
- Sidecars can be refreshed or omitted without changing executable scenario identity.
- Exact, high-confidence, ambiguous, unmatched, and not-covered outcomes remain visible instead of being collapsed into a false match.
- The approach stays application-independent and avoids Quizzes-specific hooks.

## Consequences

Current v4 dynamic output is:

```text
workload-dynamic-evidence.jsonl
workload-dynamic-evidence-manifest.json
dynamic-evidence-join-report.json
dynamic-evidence/                 # raw operational evidence
```

ScenarioExecutor does not use these sidecars to redefine a persisted action schedule. Package immutability is covered by dummyapp integration.

Dynamic enrichment remains optional and relatively expensive. A broad run is justified only when it answers a named attribution, generation, or execution question. Historical v2/v3 attribution counts are not current v4 package evidence.

## Rejected alternatives

- **Pure static exact binding everywhere:** too broad and brittle before representative need proves the value.
- **Runtime evidence as the scenario contract:** makes identity coverage-dependent and weakens reproducibility.
- **Post-run heuristic matches labelled exact:** inferred identity is not exact evidence.
- **Application-specific hooks or broad name matching:** may improve headline counts while increasing false attribution.

## Revisit when

- a representative executable scenario requires stronger runtime aggregate-key binding;
- dynamic evidence must affect selection rather than remain explanatory;
- a fresh v4 baseline shows that the sidecar boundary no longer answers the required thesis question.

Current behavior and evidence are documented in [`../current-state.md#optional-dynamic-evidence`](../current-state.md#optional-dynamic-evidence).
