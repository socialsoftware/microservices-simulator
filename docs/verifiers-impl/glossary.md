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
| Count-only catalog mode | Scenario-generation/accounting mode where the verifier computes exact compressed counts for a selected generation strategy but does not materialize every selected `ScenarioPlan` line. This is required for Quizzes-scale brute-force runs. |
| Legacy behavior CSV generator | Existing simulator impairment utility that expands manually supplied functionality/step option files into per-functionality CSV run blocks. It drives runtime fault/delay behavior but does not discover saga sets, inputs, aggregate interactions, or scenario schedules, so it is not the thesis scenario-space baseline. |
| Interacting scenario universe | The subset of the brute-force scenario universe whose saga instances can affect each other through shared aggregate instances or shared aggregate-type evidence. This is the intended useful-space/oracle approximation for evaluating scenario-generation pruning. |
| Strict interaction universe | High-confidence interacting subset requiring concrete or clearly shared symbolic interaction evidence and at least one write. Unknown/type-only matches are excluded. |
| Broad interaction universe | Over-approximated interacting subset that also allows type-only or unknown-key fallback evidence when configured. This may include false positives, but it is useful as an upper estimate of potentially useful scenarios. |
| Generated interacting catalog | The actual `ScenarioPlan` set emitted by the generator after applying its implemented interaction model, input compatibility rules, schedule strategy, and configured caps. |
| Scenario-space accounting | A report that quantifies scenario-shape counts across the input-bound brute-force universe, the configured generator-selected subset, and the written catalog. The current v1 scope is static accounting; executor-admissible and executed fault-free counts are future extensions. |
| Compressed accounting | Exact, factorized scenario-space counting that groups candidates by saga set and computes input-tuple and schedule counts mathematically instead of materializing every `ScenarioPlan`. It preserves count-level information for large applications such as Quizzes while avoiding infeasible per-scenario dumps. |
| Generation strategy | Configured rule deciding which scenario shapes are emitted as `ScenarioPlan` records, such as the current interaction-pruned generator or a future brute-force baseline generator. |
| Accounting lens | Report-only classification applied to scenario-space candidates, such as all brute-force candidates, strict interacting candidates, conservative interacting candidates, generated candidates, executor-admissible candidates, and executed candidates. Accounting lenses do not themselves decide which plans are emitted. |
| Schedule strategy | Configured rule for deriving step orderings for a fixed saga set and input tuple. Current implemented behavior supports serial ordering and bounded order-preserving interleavings; thesis-style segment compression should be treated as not implemented until a real compression algorithm exists. |
| Scenario catalog | Deterministic machine-readable output describing static scenario plans and rejected inputs for later execution or enrichment. |
| `ScenarioPlan` | A generated static scenario candidate, including participating saga instances, input variants, footprints, schedules, and stable identifiers. It may expose a `FaultSpace` describing executable fault slots, but each fault vector is not a separate `ScenarioPlan`. |
| Scenario plan id | Stable identifier for a generated `ScenarioPlan`, used to connect static catalog entries, reports, and enrichment sidecars. |
| Input variant | A static representation of one observed way a test constructs or invokes a saga/functionality input path. |
| `InputVariant` id | Stable identifier for a static input variant; when runtime evidence carries it, dynamic enrichment can join evidence exactly. |
| Saga construction recipe | Extracted trace of how a Groovy test constructs or obtains saga inputs, including literals, helper calls, facade calls, transforms, placeholders, and unresolved values. |
| Source mode | Classification of test input source evidence as `SAGAS`, `TCC`, `MIXED`, or `UNKNOWN`. The scenario catalog accepts saga-compatible inputs and rejects unsupported TCC/mixed candidates diagnostically. |
| Footprint | Static approximation of what a saga step or command touches, usually aggregate type plus access mode and, when available, key evidence. |
| Access policy | Extracted command-handler or domain-service behavior describing aggregate reads/writes and registration patterns. |
| Static confidence | Confidence label attached to static evidence when exact instance binding is unavailable or conservative. |

## Dynamic enrichment terms

| Term | Meaning |
|---|---|
| Dynamic enrichment | Optional verifier workflow that runs selected application tests, collects simulator runtime evidence, and writes sidecar-enriched catalog artifacts without changing the static catalog. |
| Runtime evidence | JSONL records emitted by simulator hooks during test execution, such as step events, aggregate observations, and attribution diagnostics. |
| Dynamic input map | Per-test-class map written by the verifier before dynamic runs so simulator hooks can map runtime test identity, functionality, and step names back to static input variants. |
| Sidecar artifact | Output that enriches or explains the static catalog without redefining it, such as `scenario-catalog-enriched.jsonl` or `dynamic-evidence-join-report.json`. |
| Join status | Category describing how runtime evidence matched a static scenario plan. Current statuses include `MATCHED_EXACT`, `MATCHED_HIGH_CONFIDENCE`, `MATCHED_PARTIAL`, `AMBIGUOUS`, `UNMATCHED`, and `NOT_COVERED`. |
| `MATCHED_EXACT` | Runtime evidence carried a direct `inputVariantId` belonging to the scenario plan. This is the strongest current join result. |
| `MATCHED_HIGH_CONFIDENCE` | Runtime evidence matched through test identity and semantic shape, but did not carry a direct input variant id. |
| `MATCHED_PARTIAL` | Runtime evidence matched some relevant shape but not enough for high-confidence attribution. |
| `AMBIGUOUS` | Runtime evidence is relevant but maps to multiple possible static input variants or scenario candidates; the verifier does not guess. |
| `UNMATCHED` | Runtime evidence exists but cannot be joined usefully to the static scenario candidate. |
| `NOT_COVERED` | No useful runtime evidence was observed for the static scenario candidate in the selected dynamic run. |

## Execution and future-work terms

| Term | Meaning |
|---|---|
| Scenario execution | Runtime stage that materializes catalog inputs and executes a scenario schedule. The current runner is a narrow POC for supported single-saga plans, not arbitrary catalog replay. |
| Fault configuration | Execution-time choice of injected faults or step-level failure choices applied to a scenario execution. It is not part of static scenario identity. |
| Fault-free execution | Current executor target where the scenario still carries fault slots, but the effective fault vector is the all-zero/default vector. |
| Domain-impact scoring | Planned scoring based on domain-visible outcomes such as invariant violations, compensation divergence, inconsistent final state, and business-state anomalies. |
| Local search | Planned search over fault configurations within a selected scenario, potentially using a genetic algorithm. |
| Scenario prioritization | Planned budget allocation across scenarios, potentially using contextual bandit methods such as LinUCB-style selection. |
