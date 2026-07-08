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
| Scenario | A planned saga execution shape: participating saga instance(s), their input variant(s), and the scheduled ordering of saga steps. Fault choices are not part of scenario identity; they are execution-time semantics applied later to a scenario. |
| Brute-force scenario universe | The bounded exhaustive set of scenario shapes considered for comparison: saga sets up to a configured maximum size, compatible input tuples for those saga sets, and order-preserving schedules/interleavings. Fault-vector combinations are excluded from this count for the current phase. |
| Type-level shape space | Report-only count over saga classes and aggregate-type footprints before requiring concrete input variants. It is useful for explaining workflow-shape pressure and missing input coverage, but it does not emit executable `ScenarioPlan` records and is not guaranteed to be numerically larger than input-bound scenario counts. |
| Input-bound brute-force universe | Bounded exhaustive scenario universe over accepted input variants, compatible input tuples, and schedules. This is the brute-force baseline that can emit `ScenarioPlan` records because each saga instance has a concrete input variant. |
| Same-saga multi-instance scenario | Scenario shape where the same saga class appears more than once with different inputs. This may expose important concurrency cases, but it is deferred from the current fair-comparison universe because the current generator enumerates sets of distinct saga FQNs. |
| Count-only catalog mode | Scenario-generation/accounting mode where the verifier computes exact compressed counts for a selected generation strategy but does not materialize every selected `ScenarioPlan` line. This is required for Quizzes-scale large runs. |
| Legacy behavior CSV generator | Existing simulator impairment utility that expands manually supplied functionality/step option files into per-functionality CSV run blocks. It drives runtime fault/delay behavior but does not discover saga sets, inputs, aggregate interactions, or scenario schedules, so it is not the thesis scenario-space baseline. |
| Interacting scenario universe | The subset of the brute-force scenario universe whose saga instances can affect each other through shared aggregate instances or shared aggregate-type evidence. This is the intended useful-space/oracle approximation for evaluating scenario-generation pruning. |
| Strict interaction universe | High-confidence interacting subset requiring concrete or clearly shared symbolic interaction evidence and at least one write. Unknown/type-only matches are excluded. |
| Broad interaction universe | Over-approximated interacting subset that also allows type-only or unknown-key fallback evidence when configured. This may include false positives, but it is useful as an upper estimate of potentially useful scenarios. |
| Generated interacting catalog | The actual `ScenarioPlan` set emitted by the generator after applying its implemented interaction model, input compatibility rules, schedule strategy, and configured caps. |
| Scenario-space accounting | A report that quantifies scenario-shape counts across the input-bound brute-force universe, the configured generator-selected subset, and the written catalog. The current v1 scope is static accounting; executor-admissible and executed fault-free counts are future extensions. |
| Compressed accounting | Exact, factorized scenario-space counting that groups candidates by saga set and computes input-tuple and schedule counts mathematically instead of materializing every `ScenarioPlan`. It preserves count-level information for large applications such as Quizzes while avoiding infeasible per-scenario dumps. |
| Generation strategy | Configured rule deciding which scenario shapes are emitted as `ScenarioPlan` records, such as the current interaction-pruned generator or a future brute-force baseline generator. |
| Accounting lens | Report-only classification applied to scenario-space candidates, such as all brute-force candidates, strict interacting candidates, conservative interacting candidates, generated candidates, executor-admissible candidates, and executed candidates. Accounting lenses do not themselves decide which plans are emitted. |
| Schedule strategy | Configured rule for deriving step orderings for a fixed saga set and input tuple. Current implemented behavior supports serial ordering, bounded order-preserving interleavings, and conflict-anchor segment-compressed scheduling. |
| Scheduled step | A catalog step occurrence in a scenario plan's expanded schedule, including its saga instance and deterministic scheduled-step identity. |
| Conflict anchor | A saga step that participates in at least one configured cross-saga conflict candidate within the current selected saga set. The strict lens uses concrete/symbolic static conflict evidence; the broad lens also includes configured type-only or unknown fallback evidence. Read/read pairs are not conflict anchors under current conflict semantics. |
| Anchor segment | Deterministic in-saga run of non-anchor steps since the previous conflict anchor, followed by the anchor step itself. Segment-compressed schedules interleave these anchor segments rather than interleaving every step independently. |
| Segment-compressed scheduling | Schedule strategy for a fixed selected saga set and input tuple that preserves order cases over conflict anchors while collapsing non-anchor/internal step permutations. Non-anchor tail steps after the final anchor, including all steps of zero-anchor sagas in mixed sets, are appended once in canonical deterministic order after all anchor segments. This is static compression under verifier conflict evidence, not proof of semantic completeness or exact runtime aggregate-instance binding. |
| Scenario catalog | Deterministic machine-readable output describing static scenario plans and rejected inputs for later execution or enrichment. |
| `ScenarioPlan` | A deterministic structural verifier record describing a generated static scenario candidate: participating saga instances, accepted input variants, footprints, schedule, conflict evidence, and stable identifiers. It may expose a `FaultSpace` describing executable fault slots, but each fault vector is not a separate `ScenarioPlan`. |
| Scenario plan id | Stable identifier for a generated `ScenarioPlan`, used to connect static catalog entries, reports, and enrichment sidecars. |
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

## Dynamic enrichment terms

| Term | Meaning |
|---|---|
| Dynamic enrichment | Optional verifier workflow that runs selected application tests, collects simulator runtime evidence, and writes sidecar-enriched catalog artifacts without changing the static catalog. |
| Dynamic enrichment sidecar | Runtime-evidence attribution artifact attached next to the static catalog. It may guide investigation and matching, but it does not create static inputs or redefine static scenario structure. |
| Runtime evidence | JSONL records emitted by simulator hooks during test execution, such as step events, aggregate observations, and attribution diagnostics. |
| Dynamic input map | Run-level map written by the verifier before dynamic runs so simulator hooks can map runtime test identity, functionality, and step names back to static input variants. |
| Sidecar artifact | Output that enriches or explains the static catalog without redefining it, such as `scenario-catalog-enriched.jsonl` or `dynamic-evidence-join-report.json`. |
| Join status | Category describing how runtime evidence matched a static scenario plan. Current statuses include `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `MATCHED_PARTIAL`, `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED`. |
| `MATCHED_EXACT` | Runtime evidence carried a direct `inputVariantId` belonging to the scenario plan. This is the strongest current join result. |
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
| ScenarioExecutor materializability | Input-level accounting that asks whether the current ScenarioExecutor can materialize an accepted input using its supported constructors, placeholders, and runtime-owned argument handling. It does not mean all accepted inputs can replay or that fault execution is implemented. |
| Runtime-owned argument resolution | ScenarioExecutor behavior where infrastructure/runtime dependencies, such as saga unit-of-work or messaging gateway arguments, are supplied by the executor/runtime rather than reconstructed from the static Groovy recipe. |
| Scenario execution | Runtime capability/stage that materializes catalog inputs and executes a scenario plan schedule. The current implementation supports a narrow single-saga fault-vector path, not arbitrary catalog replay. |
| Scenario execution attempt | One concrete runtime attempt to execute one scenario plan with one assigned fault vector in one application/runtime environment. A scenario execution attempt receives its own `scenarioExecutionId` and writes one execution report. |
| Fault configuration | Execution-time choice of injected faults or step-level failure choices applied to a scenario execution attempt. It is not part of static scenario identity. |
| Fault space | The ordered set of fault slots available for a scenario plan. |
| Fault slot | One indexed faultable forward scheduled step in a scenario plan's fault space. |
| Fault vector | A binary string assigned over every fault slot in a scenario plan's fault space. |
| Assigned vector | The fault vector selected for one scenario execution attempt. |
| Realized fault slot | A fault slot whose assigned `1` bit is reached at runtime and injects the expected simulator fault. |
| Masked fault slot | A fault slot whose assigned `1` bit is not reached because an earlier realized fault terminates the relevant saga instance. |
| Fault-free execution | Current executor target where the scenario still carries fault slots, but the effective fault vector is the all-zero/default vector. |
| ScenarioExecutor supported path | The currently implemented runtime slice: one materializable single-saga saga/local scenario execution attempt per run, with either the plan default vector or one explicit binary fault vector, producing a standalone v2 execution report. |
| Domain-impact scoring | Planned scoring based on domain-visible outcomes such as invariant violations, compensation divergence, inconsistent final state, and business-state anomalies. |
| Local search | Planned search over fault configurations within a selected scenario, potentially using a genetic algorithm. |
| Scenario prioritization | Planned budget allocation across scenarios, potentially using contextual bandit methods such as LinUCB-style selection. |
