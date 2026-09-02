# Plan: current-only verifier artifacts

## Environment and execution mode

- **Documentation route:** documented (`SPEC.md` and `PLAN.md`), supported by the
  completed artifact audit.
- **Execution rigor:** milestone-reviewed. The change crosses accounting, package
  models, readers, on-demand mutation, dynamic attribution, preflight, and execution.
- **Isolation:** a dedicated worktree after explicit implementation approval. The
  current checkout contains unrelated documentation and untracked work that must remain
  untouched.
- **Coordination:** one implementation task owns the complete sequence and uses bounded
  subagents only for milestone work that can be independently reviewed. One consistent
  reviewer checks every milestone against the same spec and examples.
- **Commits:** one normal commit after final review. No push, merge, PR, or handoff back
  to `fault-analysis/scenarios` without separate user direction.
- **User verdict:** none between milestones. The artifact shapes were settled during the
  audit; passed milestones continue in order.

No worktree, source implementation, generated benchmark replacement, or commit is
authorized by this plan alone.

## Implementation strategy

Build the current package from its simplest useful form outward. First make count-only
produce the role-keyed manifest, compact accounting, and authoritative static facts.
Then make catalog-writing add normalized setups, WorkloadPlans, FaultScenarios, and
on-demand requests while adapting the reader and executor. Finally replace the dynamic
sidecar with observation and Saga-invocation attribution streams.

The package `formatVersion` selects one complete contract. New writers emit only that
contract and ordinary readers support only that contract; no legacy dispatch layer or
field blacklist is introduced. One minimal set of serialization fixtures is the shared
source for exact-shape tests and readable examples, while readers validate required
fields, paths, hashes, identities, and references.

Static analysis, counting, schedule generation, fault enumeration, setup execution, and
impact behavior retain their present algorithms. The dynamic join keeps its current
matching evidence but changes the stored unit from WorkloadPlan summaries to runtime
Saga-invocation/input attributions. The confirmed input-map mismatch and other audit
findings remain visible rather than being repaired inside this change.

Cross-file references resolve through one package-reader boundary. Saga steps and event
routes use Saga-local ids; WorkloadPlan participants and schedule occurrences use
plan-local ids. Opaque ids remain for inputs, interactions, setups, WorkloadPlans,
FaultScenarios, and runtime observations where selection or cross-file reference
requires them. A request is identified by its WorkloadPlan, vector, and effective cap.

On-demand generation continues to lock and publish package changes atomically. It reads
the default recovery cap from accounting, accepts an explicit override, records the
effective request, preserves initial fault totals, updates current totals, and refreshes
the affected hashes.

The audit remains the single findings ledger. Execution may clarify a confirmed finding
but must not absorb static-analysis, materialization, CLI, impact, or GA work. After the
fresh baseline, the final handoff recommends the next issue based on evaluation validity
and executable coverage; it does not pre-create a backlog of specs.

Keep the implementation thesis-sized: extend the existing concrete models, adapters,
writers, and reader rather than introducing a generic artifact framework. Do not add a
compatibility or migration layer, JSONL index or database, configurable validation
strictness, duplicated documentation fixtures, or opportunistic fixes for deferred
audit findings.

## Milestones M0–M4

### M0 — Freeze examples and comparison evidence

#### Outcome and spec coverage

Establish executable serialization examples and the static totals that subsequent
milestones must preserve. Covers FR-1–FR-2, FR-8–FR-17, and the data-model identities
used throughout the spec.

#### Change boundary

Shared fixture files and issue references only. Production output remains unchanged.

#### Known anchors

- `issues/2026-08-30-current-only-verifier-artifacts/AUDIT.md`
- `issues/2026-08-30-current-only-verifier-artifacts/examples/`
- retained Quizzes count-only and v4 catalog runs under `verifiers/target/`
- `ScenarioSpaceAccountingReport`
- `ScenarioCatalogManifest`

#### Discovery / preflight

Reconfirm the current branch and dirty state, map each approved example field to an
existing analysis/model source, and identify the exact retained run used for numerical
comparison. Resolve only mechanical example omissions; any new domain meaning returns
to the user.

#### Implementation strategy

Create one minimal set of compact representative fixture files for every current
package role, including conditional count-only, catalog-writing, dynamic, and
on-demand fields. Reuse those files directly in exact-shape tests and refer to them from
the issue instead of maintaining a second copy. Capture the known Quizzes static totals
and equations without copying the old combination rows.

#### Proof before continuing

- Every approved package entity has one readable representative example.
- Cross-file ids in the examples resolve.
- The baseline includes the agreed Saga, input, materializability, interaction,
  workload, and computed-vector totals.
- No example contains an unexplained placeholder field or duplicate owner.

### M1 — Current count-only package

#### Outcome and spec coverage

Count-only produces the role-keyed manifest, compact accounting, and normalized Saga,
input, and direct-interaction facts. Covers FR-1–FR-3 and FR-6–FR-25.

#### Change boundary

Accounting calculation/model/writing, static fact adapters and writers, count-only
manifest generation, current package reading for static artifacts, dummyapp fixtures,
and focused documentation. Catalog-writing, on-demand mutation, dynamic enrichment,
and execution behavior remain unchanged until later milestones.

#### Known anchors

- `verifiers/.../scenario/accounting/`
- `ApplicationAnalysisState`
- `ApplicationAnalysisScenarioModelAdapter`
- `ScenarioCatalogJsonlWriter`
- `ScenarioCatalogPackageReader`

#### Discovery / preflight

Trace which approved Saga, input, interaction, event-route, setup, and accounting facts
already exist in analysis state and which currently exist only inside WorkloadPlans.
Confirm stable source ordering before assigning local step and route ids.

#### Implementation strategy

Introduce current artifact records at the writer boundary and adapt existing analysis
objects without changing their conclusions. Calculate connected Saga-set totals without
retaining the concrete sets. Write only present count-only roles and validate their
paths and hashes through the current reader.

#### Proof before continuing

- Exact writer-shape tests match the approved manifest, accounting, Saga, input, and
  interaction examples.
- Required-field, path-boundary, hash, duplicate-id, and dangling Saga/step reference
  failures are rejected.
- Dummyapp positive and negative fixtures cover accepted, rejected, materializable,
  blocked, strict, and fallback-only facts.
- Equivalent Quizzes configuration preserves the agreed static totals and accounting
  equations.
- Accounting contains no concrete Saga-set, WorkloadPlan, vector, schedule, or runtime
  rows.
- Artifact sizes are reported for inspection without a byte-size pass/fail gate.

### M2 — Current executable package and on-demand mutation

#### Outcome and spec coverage

Catalog-writing adds reusable setups, reference-based WorkloadPlans, compact
FaultScenarios, and package-level requests; preflight, execution, and on-demand
generation consume the current package. Covers FR-4, FR-6–FR-7, FR-14–FR-15,
FR-26–FR-32, and FR-40.

#### Change boundary

Setup, WorkloadPlan, FaultScenario, request, and manifest models; catalog writers;
central reader/reference validation; on-demand CLI/service; setup preflight;
ScenarioExecutor package selection; bounded benchmark fixtures; and focused canonical
documentation. Static analysis conclusions, scheduling algorithms, recovery
enumeration, executor semantics, ImpactV1, and application predicates are excluded.

#### Known anchors

- `WorkloadPlan`
- `SetupPlan`
- `FaultScenario`
- `ScenarioCatalogManifest`
- `ScenarioCatalogPackageReader`
- `OnDemandFaultScenarioService`
- `ScenarioExecutor`

#### Discovery / preflight

Trace every consumer of embedded inputs, setup plans, conflict evidence, fault slots,
event consequences, and compensation checkpoints. Confirm that each consumer can use
the approved reference or local-occurrence replacement without reconstructing removed
metadata heuristically.

#### Implementation strategy

Write reusable setups before WorkloadPlans, then WorkloadPlans before FaultScenarios.
Resolve selected records through one validated reader result. Preserve semantic identity
from referenced content rather than file position. Replace eager and on-demand action
metadata with references to the WorkloadPlan schedule. Publish on-demand changes through
temporary files under the existing lock and replace the affected package files only
after all new hashes and references validate.

#### Proof before continuing

- Exact writer-shape tests cover source-derived and provider-backed setups, step and
  event schedules, FaultScenario actions, and request records.
- Missing/duplicate input, interaction, setup, workload, occurrence, route, scenario,
  and request references fail with concise errors.
- A count-only package remains readable without executable-role placeholders.
- One dummyapp and one supported bounded Quizzes package reach setup-ready preflight and
  replay the selected persisted FaultScenario with unchanged behavior.
- Catalog-writing initially produces equal `faultScenarios.initial` and `current`.
- A new on-demand vector uses the accounting cap by default; an override is recorded;
  exact repeats deduplicate; different caps may add only previously absent schedules.
- Successful requests update the request stream, fault catalog, current totals, and
  manifest hashes together. Failed requests and simulated publication failures leave
  the original package intact.

### M3 — Dynamic observations and input attribution

#### Outcome and spec coverage

Optional catalog-writing enrichment stores each runtime observation once, attributes
Saga invocations to static inputs, and reports input and workload participant evidence
without a workload-shaped sidecar. Covers FR-5, FR-16, and FR-33–FR-39.

#### Change boundary

Dynamic event normalization, join result grouping, observation and attribution writers,
dynamic accounting, manifest composition, and dynamic integration tests. Simulator
instrumentation, test discovery, the runtime input-map mismatch, and static input
inference are excluded.

#### Known anchors

- `DynamicEvidenceReader`
- `DynamicEvidenceJoiner`
- `DynamicEnrichmentOrchestrator`
- `EnrichedScenarioCatalogWriter`
- `DummyappDynamicEnrichmentIntegrationSpec`

#### Discovery / preflight

Trace current `EventAnalysis` candidate information before it is aggregated into
WorkloadPlan rows. Confirm the grouping key is test execution plus Saga invocation and
classify events without test, Saga, invocation, or step context. Confirm that every
retained command field remains bounded by the existing instrumentation limits.

#### Implementation strategy

Normalize the five event kinds into one observation stream. Deduplicate observation ids
and group attributable events by test/Saga invocation. Apply the existing exact,
test-and-shape, shape-only, ambiguous, and unmatched decisions to that group. Derive
unique-input evidence by strongest attribution and WorkloadPlan participant evidence
from input references. Remove the workload dynamic sidecar, its manifest, and the join
report from package output; keep logs, reports, and runtime input maps in diagnostic
output. Treat the raw simulator event stream as temporary normalization input: remove
it after the normalized package is finalized successfully and retain it when
normalization fails.

#### Proof before continuing

- Exact shape tests cover all five observation kinds and all five attribution statuses.
- Repeated executions of one input produce several attributions but one unique observed
  input; stronger evidence wins the input category.
- Ambiguous candidates do not count as observed inputs.
- `shapeOnly` does not establish that all WorkloadPlan inputs appeared together.
- Fixtures cover all four WorkloadPlan participant-evidence categories.
- Observation bodies occur only in the observation stream; attribution records contain
  references rather than copied commands, accesses, steps, tests, or warnings.
- Static-only and non-enriched catalog runs contain no empty dynamic roles.
- The current input-map mismatch remains explicitly visible in realistic results and is
  not converted into invented exact matches.

### M4 — Realistic qualification and documentation

#### Outcome and spec coverage

The complete current-only package is demonstrated on realistic Quizzes paths, its
artifact story matches canonical documentation, and the next thesis-relevant verifier
issue is chosen from fresh evidence. Covers final composition of FR-1–FR-40.

#### Change boundary

Bounded Quizzes count-only, catalog-writing, preflight, replay, on-demand, and dynamic
smokes; artifact-size reporting; canonical verifier documentation; issue handoff; and
final review. No application semantics, impact scoring, GA behavior, deployment, push,
merge, or PR work.

#### Known anchors

- `docs/verifiers-impl/current-state.md`
- `docs/verifiers-impl/roadmap.md`
- the bounded RemoveTournament/AddParticipant setup and replay path
- `issues/2026-08-30-current-only-verifier-artifacts/AUDIT.md`

#### Discovery / preflight

Select bounded commands that exercise size-1–3 count-only analysis, one source-derived
setup workload, one on-demand multi-fault vector, and optional dynamic enrichment on the
available host without recreating an unbounded catalog.

#### Implementation strategy

Generate fresh evidence from the final code, inspect every manifest role, compare static
totals with the retained baseline, and record the actual commands and observed file
sizes. Update current-state documentation only with behavior and evidence that the final
run demonstrates. Keep confirmed deferred findings in the audit and rank their next
action by evaluation validity and executable coverage.

#### Proof before continuing

- A fresh count-only package contains exactly the applicable static roles and preserves
  the agreed numerical baseline under equivalent configuration.
- A fresh bounded catalog selects, preflights, and replays one supported Quizzes
  FaultScenario.
- A fresh on-demand request persists and selects a requested multi-fault vector under
  the effective cap.
- A bounded dynamic run writes observations and attributions once and reports its
  current exact-input limitation honestly.
- Actual artifact record counts and byte sizes are reported as observations.
- Canonical documentation names only files and commands the final implementation
  produces.
- Final review finds no unresolved reference, ownership, lifecycle, or scope mismatch.

## Validation strategy

Validation has four layers:

1. **Contract fixtures:** exact serialized keys and representative values for every
   artifact, plus negative validation for required fields, identities, paths, hashes,
   and references. Unknown-field blacklists and legacy-record fixtures are unnecessary;
   the manifest format selects the current contract.
2. **Dummyapp behavior:** precise positive and negative coverage for static facts,
   accounting equations, reference resolution, setup reconstruction, FaultScenario
   action validation, on-demand mutation, and dynamic attribution categories.
3. **Quizzes composition:** equivalent size-1–3 static counts, one bounded executable
   package, one on-demand vector, and one bounded dynamic smoke where host resources
   permit.
4. **Repository checks:** focused tests after each milestone, `mvn -q test` from
   `verifiers/`, compilation/test-compilation where useful, and `git diff --check`.

The final report includes commands, test totals, retained numerical comparisons, and
artifact sizes. Size is diagnostic evidence rather than an acceptance threshold.

## Risks and fallbacks

| Risk | Detection | Mitigation / fallback |
|---|---|---|
| A field is removed before its necessary fact has a new owner | An approved example or consumer cannot be populated without guessing | Stop at the smallest missing fact; restore ownership in the relevant entity or return for a domain decision |
| New references break setup or replay | Reader/preflight/executor fixture or bounded Quizzes smoke fails | Correct the central resolution boundary before continuing; do not re-embed the removed objects as a shortcut |
| Static totals change during a format-only milestone | Dummyapp or equivalent Quizzes comparison differs | Compare the smallest owning section and keep the existing analysis/counting algorithm |
| Current-only removal invalidates tests that depend on retained packages | A fixture opens v4/v5 data rather than generating current data | Replace it with a current generated fixture or preserve the historical result as documentation, not a runtime compatibility layer |
| On-demand mutation publishes a partial package | Failure-injection test observes mixed hashes or records | Keep the existing lock, stage all affected files, validate the staged package, and roll back before replacement on failure |
| Invocation-level grouping silently changes matching meaning | Controlled attribution fixtures differ before grouping | Preserve candidate evidence and change only the aggregation unit; any needed inference change returns to the user |
| Dynamic output is treated as reproducible static evidence | Repeated runs differ in timestamps, ordering, or ids | Document the observed-run boundary and compare semantic counts rather than byte identity |
| The change expands into analyzer, materialization, CLI, impact, or GA work | A milestone requires one of the confirmed deferred findings to pass | Record the dependency and return to the user; do not absorb the follow-up |
| Realistic qualification consumes excessive time or disk | Selected run exceeds its declared caps or projected output | Stop the run safely, keep completed evidence, and reduce only the bounded workload/dynamic selection without weakening contract tests |
