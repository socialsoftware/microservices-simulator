# M0 handoff — current-only artifact examples and baseline

- **State:** complete; waiting for M1 review.
- **Outcome:** M0 preflight and one shared representative fixture are frozen. No
  production writer, reader, analyzer, executor, application, or generated package
  was changed.
- **Unrelated state preserved:** the pre-existing
  `docs/verifiers-impl/current-state.md` edit and untracked `lib/` and `tmp/`
  remain untouched.

## Shared fixture

[`current-package.fixture.json`](../2026-08-30-current-only-verifier-artifacts/examples/current-package.fixture.json)
is the one source later exact-shape tests can reuse. It contains representative
records for accounting, Sagas, inputs, direct interactions, setups, WorkloadPlans,
FaultScenarios, on-demand requests, runtime observations, and attribution links.

It materializes four independent accounting/manifest variants:

| Variant | Accounting shape | Role difference |
| --- | --- | --- |
| `count-only` | no executable or dynamic sections and no inapplicable caps | accounting, Saga, input, interaction only |
| `initial-catalog` | `faultScenarios.initial == current`; no dynamic section | adds setups, WorkloadPlans, FaultScenarios, empty request stream |
| `dynamic-enriched-catalog` | initial catalog accounting plus `dynamicEvidence` | adds observations and attribution links |
| `post-on-demand` | initial totals preserved; current totals increase | adds one request and one current FaultScenario |

Every manifest entry contains only `path` and `sha256`. JSON is compact UTF-8 with
no final newline. JSONL is one compact object per line with exactly one trailing LF;
the empty request stream is zero bytes. The fixture stores structured role records
only. Validation/tests mechanically derive those exact per-role bytes and compare
their SHA-256 values with the manifest's retained expected hashes, so later
writers/tests reuse the records and byte convention directly rather than hash
serialized arrays or merge/remove metadata.

The static examples include Saga-local per-step limitation fields (with one
illustrative limitation and no duplicated top-level Saga limitations), exact and
canonical `sameSource` input key evidence, and a direct write/read interaction on
the same `order` aggregate. The accepted blocked runtime-dependent Payment input
and rejected partial input remain non-executable facts with their own blockers; a
separate fully resolved, materializable `PaymentDto` input is used by every
WorkloadPlan and FaultScenario. Accounting has per-Saga rows,
`affectedInputsByReason`, event emission/resolved-route metrics, and the exact JSON
integer `118264581564861424`. Each dynamic attribution links only to observations
from its own test execution, Saga, and invocation; each observation body is stored
once. Fault actions cover all forward occurrences, let survivors proceed, and never
compensate a step whose vector bit faulted before completion.

## Retained Quizzes comparison

The exact retained report was inspected read-only outside this worktree:

`/Users/andre/meic/thesis/microservices-simulator/verifiers/target/overnight-20260827-232754/census-strict-size3/quizzes-20260828-000834-407/scenario-space-accounting.json`

Its SHA-256 is
`6416f7faa4e1e49291ac2dd8a0657f6bd48727cfa9ffa51270f278f9964e9ea1`; it is
16,144,724 bytes and 231,649 lines. The retained run is `COUNT_ONLY`,
`INTERACTION_PRUNED`, size 3, `includeSingles=false`, input cap 1000, schedule cap
20, catalog cap 100, strict interaction selection, seed 1234.

| Metric | Retained value |
| --- | ---: |
| discovered Sagas (with / without accepted inputs) | 68 (36 / 32) |
| accepted / rejected source inputs | 777 / 86 |
| static materializable / blocked inputs | 91 / 686 |
| strict interactions (covered / missing) | 140 (63 / 77) |
| strict connected sets (size 2 / 3) | 140 / 1,299 |
| fallback interactions (covered / missing) | 540 (223 / 317) |
| fallback connected sets (size 2 / 3) | 540 / 7,005 |
| all input-bound space (size 2 / 3) | 1,161,251,056 (2,315,128 / 1,158,935,928) |
| selected input-bound space (size 2 / 3) | 42,079,271 (516,651 / 41,562,620) |
| catalog-written input-bound space | 0 |
| WorkloadPlans (materializable) | 3 (3) |
| event consequences / normal actions written | 2 / 13 |
| FaultScenarios / eager vectors / on-demand vectors | 22 / 14 / 0 |
| recovery schedules (uncapped / written) | 22 / 22 |

Preserved equations:

```text
777 + 86 = 863 discovered source input records
777 = 91 + 686 accepted-input materializability funnel
140 = 63 + 77 strict interaction evidence split
540 = 223 + 317 fallback interaction evidence split
2,315,128 + 1,158,935,928 = 1,161,251,056 all input-bound
516,651 + 41,562,620 = 42,079,271 selected input-bound
```

Recovery totals retain the report's
`EXACT_SUM_OVER_COMPUTED_VECTORS_ONLY` qualification; they are not an all-vector
claim. No old combination rows were copied.

## Proof and classifications

The local `verifiers/target/` directory is absent, while the retained report above
is available externally. This is an environmental limitation, not a scope delta;
no local Quizzes rerun was claimed. Maven was not run because this milestone changes
only issue examples and handoff documentation, with no production path to compile.

Executed checks:

```text
jq empty issues/2026-08-30-current-only-verifier-artifacts/examples/current-package.fixture.json  PASS
inline Python fixture validator (derived compact JSON/JSONL bytes vs expected hashes, ids, equations, materializability, attribution groups, fault orders)  PASS
git diff --check  PASS
```

Autonomous decisions: retain one shared fixture source; encode the four complete
variants rather than derive them by merge/remove operations; use the external retained
Quizzes report by canonical path, hash, and aggregate values; and defer writer,
reader, analysis, execution, dynamic-join, input-map, CLI, impact, and search work to
their approved milestones. No material scope delta was found.
