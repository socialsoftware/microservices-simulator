# Verifier glossary

This glossary defines recurring verifier terms used across the knowledge base. Use it with [`current-state.md`](current-state.md), [`roadmap.md`](roadmap.md), and the dynamic-enrichment docs when refreshing terminology.

## Scope and status labels

| Term | Meaning |
|---|---|
| Current truth | Present-tense implementation status. In this knowledge base, [`current-state.md`](current-state.md) is the canonical source. |
| Implemented | Behavior that exists in the repository and has at least local verifier coverage or a documented smoke run. |
| Partial | Behavior that exists but has known scope, confidence, or coverage limits. |
| Deferred | Planned or useful work that is intentionally not part of the current baseline. |
| Out of scope | Work that should not be claimed or pursued under the current verifier slice unless the scope changes. |
| Archive | Historical or superseded material kept for provenance. It can support history, but not current claims by itself. |

## Static analysis and scenario terms

| Term | Meaning |
|---|---|
| Verifier | The `verifiers/` module that analyzes simulator applications and tests to generate fault-analysis scenario material. |
| Scenario | Broad historical term for a planned or executed Saga experiment. In current v3 documentation, use `WorkloadPlan` for reusable normal structure, `FaultScenario` for one persisted vector/action schedule, and execution attempt for one measured run. `ScenarioPlan` refers only to the superseded v1/v2 contract. |
| Brute-force scenario universe | The bounded exhaustive set of workload shapes considered for comparison: saga sets up to a configured maximum size, compatible input tuples, and forward schedules/interleavings. Fault vectors and recovery schedules are counted separately in v3. |
| Type-level shape space | Report-only count over saga classes and aggregate-type footprints before requiring concrete input variants. It explains workflow-shape pressure and missing input coverage, but it does not emit WorkloadPlans and is not guaranteed to exceed input-bound counts. |
| Input-bound brute-force universe | Bounded exhaustive workload universe over accepted input variants, compatible input tuples, and forward schedules. This baseline can emit WorkloadPlans because each saga instance has a concrete input variant. |
| Same-saga multi-instance workload | Workload shape where the same saga class appears more than once with different inputs. It may expose important concurrency cases, but it is deferred from the current fair-comparison universe because the generator enumerates sets of distinct saga FQNs. |
| Count-only catalog mode | Generation/accounting mode where the verifier computes exact compressed WorkloadPlan counts for a selected strategy without writing every selected workload line. FaultScenario recovery accounting is exact only for vectors that are actually computed. |
| Legacy behavior CSV generator | Existing simulator impairment utility that expands manually supplied functionality/step option files into per-functionality CSV run blocks. It drives runtime fault/delay behavior but does not discover saga sets, inputs, aggregate interactions, or scenario schedules, so it is not the thesis scenario-space baseline. |
| Interacting scenario universe | The subset of the brute-force scenario universe whose saga instances can affect each other through shared aggregate instances or shared aggregate-type evidence. This is the intended useful-space/oracle approximation for evaluating scenario-generation pruning. |
| Strict interaction universe | High-confidence interacting subset requiring concrete or clearly shared symbolic interaction evidence and at least one write. Unknown/type-only matches are excluded. |
| Broad interaction universe | Over-approximated interacting subset that also allows type-only or unknown-key fallback evidence when configured. This may include false positives, but it is useful as an upper estimate of potentially useful scenarios. |
| Generated interacting catalog | The WorkloadPlan set emitted after applying the interaction model, input compatibility rules, forward-schedule strategy, and configured caps. |
| Scenario-space accounting | The v3 report that quantifies workload shape space and written WorkloadPlans, records per-workload materializability/vector space, and reports exact uncapped/written recovery counts for eager or on-demand computed vectors. It does not claim an all-vector recovery total when that space was not computed. |
| Compressed accounting | Exact, factorized workload-space counting that groups candidates by saga set and computes input-tuple and forward-schedule counts without materializing every WorkloadPlan. Recovery scheduling uses separate exact BigInteger counting with bounded retained schedules. |
| Generation strategy | Configured rule deciding which workload shapes are emitted as WorkloadPlans, currently `INTERACTION_PRUNED` or `BRUTE_FORCE`. |
| Accounting lens | Report-only classification applied to workload-space candidates, such as all brute-force, strict interacting, conservative interacting, generated, executor-admissible, and executed candidates. Accounting lenses do not decide which records are emitted. |
| Schedule strategy | Configured rule for deriving step orderings for a fixed saga set and input tuple. Current implemented behavior supports serial ordering, bounded order-preserving interleavings, and conflict-anchor segment-compressed scheduling. |
| Scheduled step | A forward step occurrence in a WorkloadPlan's global forward schedule, including its saga instance and deterministic occurrence identity. |
| Conflict anchor | A saga step that participates in at least one configured cross-saga conflict candidate within the current selected saga set. The strict lens uses concrete/symbolic static conflict evidence; the broad lens also includes configured type-only or unknown fallback evidence. Read/read pairs are not conflict anchors under current conflict semantics. |
| Anchor segment | Deterministic in-saga run of non-anchor steps since the previous conflict anchor, followed by the anchor step itself. Segment-compressed schedules interleave these anchor segments rather than interleaving every step independently. |
| Segment-compressed scheduling | Schedule strategy for a fixed selected saga set and input tuple that preserves order cases over conflict anchors while collapsing non-anchor/internal step permutations. Non-anchor tail steps after the final anchor, including all steps of zero-anchor sagas in mixed sets, are appended once in canonical deterministic order after all anchor segments. This is static compression under verifier conflict evidence, not proof of semantic completeness or exact runtime aggregate-instance binding. |
| Scenario package | The five-file deterministic v3 machine contract: workload catalog, FaultScenario catalog, rejected-input diagnostics, accounting, and manifest. The manifest links artifact paths, exact counts, hashes, policy, materializability diagnostics, and the recovery cap. |
| `WorkloadPlan` id | Stable identifier for reusable normal-execution structure; dynamic evidence sidecars and FaultScenarios link through it. |
| `FaultScenario` id | Stable identifier for one workload id, assigned vector, and ordered action schedule. It is the required executor selection key. |
| `ScenarioPlan` | Superseded v1/v2 single-record contract. It may appear in explicitly historical evidence and ADRs, but current package generation, enrichment, and execution use WorkloadPlan/FaultScenario v3 records. |
| Input variant | A static representation of one observed way a test constructs or invokes a saga/functionality input path. |
| Static accepted input coverage | Whether a discovered saga has at least one accepted static `InputVariant` after source-mode and policy filtering. This is not evidence that the input is executor-ready, replayable, or dynamically exercised. |
| Event-origin input | Static `InputVariant` produced from an event-driven chain, such as the implemented `EventHandling` method -> `EventProcessing` method -> facade/functionality -> saga shape. |
| Event payload placeholder | Placeholder in an event-origin recipe where the static extractor knows an event payload value is needed but cannot reconstruct the concrete runtime object/value. It may allow static acceptance while blocking materialization. |
| `InputVariant` id | Stable identifier for a static input variant; when runtime evidence carries it, dynamic enrichment can join evidence exactly. |
| Input provenance | Static source information describing where an input was created (`sourceClassFqn`, `sourceMethodName`, binding name, source/provenance text). Helper provenance is preserved even when runtime ownership belongs to a Spock feature. |
| Input owner | Test class/method identity eligible to match runtime evidence for an input. Owners constrain attribution; they are not proof by themselves. Helper-created inputs can keep helper provenance while being owned by the setup or feature that called the helper. |
| Call context | Surrounding method context for an input (`callContextMethodName`), used to distinguish a helper method from the setup/feature context that called it. |
| Input role | Additive classification of a static input as `FEATURE_UNDER_TEST`, `FIXTURE_PREREQUISITE`, or `UNKNOWN`. |
| Fixture origin | Additive classification of fixture inputs such as `SETUP`, `SETUP_HELPER`, `FIELD`, `SETUP_SPEC`, inherited fixture variants, or `DIRECT_FEATURE`. |
| Saga construction recipe | Extracted trace of how a Groovy test constructs or obtains saga inputs, including literals, helper calls, facade calls, transforms, placeholders, and unresolved values. |
| Source mode | Classification of test input source evidence as `SAGAS`, `TCC`, `MIXED`, or `UNKNOWN`. The scenario catalog accepts saga-compatible inputs and rejects unsupported TCC/mixed candidates diagnostically. |
| Footprint | Static approximation of what a saga step or command touches, usually aggregate type plus access mode and, when available, key evidence. |
| Access policy | Extracted command-handler or domain-service behavior describing aggregate reads/writes and registration patterns. |
| Static confidence | Confidence label attached to static evidence when exact instance binding is unavailable or conservative. |

## Compensation-aware v3 package terms

These terms describe the implemented contract accepted in [`decisions/2026-07-19-compensation-aware-fault-scenario-contract.md`](decisions/2026-07-19-compensation-aware-fault-scenario-contract.md). [`current-state.md`](current-state.md) remains the canonical implementation-status source.

| Term | Meaning |
|---|---|
| Workload plan | Reusable normal-execution structure containing Saga participants, accepted inputs, one deterministic global forward interleaving, conflict evidence, ordered forward fault slots, and ordered compensation checkpoints/evidence. It does not contain an assigned vector or compensation ordering. |
| Fault scenario | One reproducible experiment that references a WorkloadPlan and adds one assigned fault vector plus one concrete compensation-aware action schedule. Its identity includes the workload-plan id, vector, and ordered action identities. |
| Compensation checkpoint | Recovery action associated with a completed forward step that may have an explicit compensation, implicit Saga-state rollback, or conservatively unknown recovery effect. Checkpoints for one aborted participant execute in reverse completed-step order. |
| Compensation evidence class | Primary static reason for retaining a checkpoint, with precedence `EXPLICIT_COMPENSATION` > `IMPLICIT_SAGA_ROLLBACK` > `CONSERVATIVE_UNKNOWN`. A step is omitted only when resolved evidence proves it read-only with no explicit registration, rollback/write potential, or unresolved diagnostic. |
| Recovery schedule | Ordered insertion of compensation checkpoints into the complete residual WorkloadPlan forward sequence. It preserves reverse compensation order and the global survivor-to-survivor forward order; it does not generate a new normal forward interleaving. |
| Recovery-schedule cap | Positive configurable maximum number of recovery schedules materialized for one vector, defaulting to `20`. Capped selection preserves deterministic recovery-timing representatives before canonical fill and does not change uncapped accounting. |
| Automatic participant commit | Lifecycle rule that a participant closes and commits immediately after its final successful forward step. Commit is reported explicitly but is not an independently schedulable action. |
| Unassigned runtime fallback | Deterministic deviation policy for a zero-bit domain/simulator body or commit failure: run one immediate checkpoint-level recovery episode for that participant, skip its remaining forward actions, and continue still-valid actions for other participants while reporting planned and actual order. |
| Schedule conformance | Relation between a persisted FaultScenario and measured execution: `EXACT` for a completed planned schedule, `DEVIATED` for completed unassigned-runtime fallback, `INCOMPLETE` for a measured hard-stop prefix, and absent when measured execution never began. |

## Dynamic enrichment terms

| Term | Meaning |
|---|---|
| Dynamic enrichment | Optional verifier workflow that runs selected application tests, collects simulator runtime evidence, and writes sidecar-enriched catalog artifacts without changing the static catalog. |
| Dynamic enrichment sidecar | Runtime-evidence attribution artifact attached next to the static catalog. It may guide investigation and matching, but it does not create static inputs or redefine static scenario structure. |
| Runtime evidence | JSONL records emitted by simulator hooks during test execution, such as step events, aggregate observations, and attribution diagnostics. |
| Dynamic input map | Run-level map written by the verifier before dynamic runs so simulator hooks can map runtime test identity, functionality, and step names back to static input variants. |
| Sidecar artifact | Output that enriches or explains the semantic v3 package without redefining it, such as `workload-dynamic-evidence.jsonl`, its manifest, or `dynamic-evidence-join-report.json`. |
| Join status | Category describing how runtime evidence matched a static WorkloadPlan. Current statuses include `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `MATCHED_PARTIAL`, `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED`. |
| `MATCHED_EXACT` | Runtime evidence carried a direct `inputVariantId` belonging to the WorkloadPlan. This is the strongest current join result. |
| `MATCHED_HIGH_CONFIDENCE` | Runtime evidence matched through test identity and semantic shape, but did not carry a direct input variant id. |
| `MATCHED_PARTIAL` | Runtime evidence matched some relevant shape but not enough for high-confidence attribution. |
| `AMBIGUOUS` | Runtime evidence is relevant but maps to multiple possible static input variants or scenario candidates; the verifier does not guess. |
| Unmatched reason | Diagnostic stored only on `UNMATCHED` dynamic evidence: `FAILED_TEST_CLASS`, `NOT_SELECTED_TEST_CLASS`, `HELPER_OWNER_MISMATCH`, or `UNCLASSIFIED`. |
| `UNMATCHED` | Runtime evidence exists but cannot be joined usefully to the static scenario candidate. |
| `NOT_COVERED` | No useful runtime evidence was observed for the static scenario candidate in the selected dynamic run. |

## Execution and future-work terms

| Term | Meaning |
|---|---|
| Static recipe readiness | Input-level accounting that asks whether the extracted static recipe itself is complete enough for materialization without executor-owned/runtime-owned argument resolution. In the post-event Quizzes count-only run this is `0`, distinct from accepted input coverage and executor materializability. |
| ScenarioExecutor materializability | Workload-level v3 policy requiring every accepted participant input to pass current readiness checks and the workload to be structurally admissible. It permits eager FaultScenario generation but predicts neither domain success nor exact runtime conformance. |
| Runtime-owned argument resolution | ScenarioExecutor behavior where infrastructure/runtime dependencies, such as saga unit-of-work or messaging gateway arguments, are supplied by the executor/runtime rather than reconstructed from the static Groovy recipe. |
| Scenario execution | Runtime stage that materializes one persisted FaultScenario and replays its action schedule. Current support is narrow saga/local deterministic sequential replay, not arbitrary/distributed execution. |
| Scenario execution attempt | One concrete runtime attempt for one persisted FaultScenario in one runtime environment. It receives an `executionAttemptId` and writes one v4 report. |
| Scenario execution participant | One saga instance in an attempt. Its report entry carries identity, materialization/startup/final state, skipped forwards, and participant-local blockers. |
| Deterministic interleaving replay | Sequential replay of a persisted FaultScenario's ordered `FORWARD` and `COMPENSATION` actions. It is not true parallel/threaded execution or distributed parity. |
| Persisted recovery continuation | Assigned-fault policy in which compensation actions occur exactly where the FaultScenario schedule places them while still-valid actions of other participants may interleave. |
| Immediate checkpoint fallback | Zero-bit domain/simulator failure policy: recover that participant immediately from runtime checkpoint truth, skip its remaining forward actions, continue valid survivor actions, and report `DEVIATED`. It does not rewrite the persisted FaultScenario. |
| Fault configuration | Assigned binary vector persisted in FaultScenario identity. The executor does not accept an ad hoc vector overlay. |
| Fault space | Ordered forward fault slots owned by a WorkloadPlan. |
| Fault slot | One indexed faultable forward occurrence in a WorkloadPlan. |
| Fault vector | Binary string aligned by index to all WorkloadPlan fault slots. |
| Assigned vector | FaultScenario's persisted vector for one reproducible experiment. |
| Realized fault slot | A fault slot whose assigned `1` bit is reached at runtime and injects the expected simulator fault. |
| Masked fault slot | A fault slot whose assigned `1` bit is not reached because an earlier realized fault terminates the relevant saga instance. |
| All-zero FaultScenario | Eager persisted experiment whose assigned vector contains only zeroes and whose action schedule contains the complete normal forward sequence with no compensation actions. |
| ScenarioExecutor supported path | One persisted, materializable saga/local FaultScenario per run, explicitly selected by id and replayed sequentially into a standalone v4 action-aware report. The assigned vector/action order come only from the package. |
| Domain-impact scoring | Planned scoring based on domain-visible outcomes such as invariant violations, compensation divergence, inconsistent final state, and business-state anomalies. |
| Local search | Planned search over fault configurations within a selected scenario, potentially using a genetic algorithm. |
| Scenario prioritization | Planned budget allocation across scenarios, potentially using contextual bandit methods such as LinUCB-style selection. |
