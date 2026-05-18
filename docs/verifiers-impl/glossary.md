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
| Scenario catalog | Deterministic machine-readable output describing static scenario plans and rejected inputs for later execution or enrichment. |
| `ScenarioPlan` | A generated static scenario candidate, including participating saga steps, input variants, footprints, schedules, and identifiers. |
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
| Scenario execution | Planned future stage that materializes catalog inputs and executes saga schedules with fault configurations. The current baseline does not yet provide this runner. |
| Fault configuration | Future representation of injected faults or step-level failure choices for a scenario execution. |
| Domain-impact scoring | Planned scoring based on domain-visible outcomes such as invariant violations, compensation divergence, inconsistent final state, and business-state anomalies. |
| Local search | Planned search over fault configurations within a selected scenario, potentially using a genetic algorithm. |
| Scenario prioritization | Planned budget allocation across scenarios, potentially using contextual bandit methods such as LinUCB-style selection. |
