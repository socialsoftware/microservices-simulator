# Current-only verifier artifact audit

Evidence from the completed artifact review and subsequent dated qualifications.
The historical rankings below describe their original checkpoints, not current task
status. The active grouped queue is owned by
`docs/verifiers-impl/roadmap.md#current-next-work`; completed findings are not outstanding
work merely because their diagnosis is retained here.

## What we have agreed

- Current packages can be regenerated. We do not need old artifact readers or migrations.
- The manifest is a table of contents and integrity check.
- The package has one integer `formatVersion`. Individual files and records do not have their own versions.
- Manifest `files` is keyed by artifact role, such as `accounting`, `inputs`, or `workloads`; each present entry contains only `path` and `sha256`.
- The reader supports the current package format selected by `formatVersion`. It validates the required current shape without maintaining an old-field blacklist or historical readers.
- We will consistently use `count-only`, `catalog-writing`, `on-demand generation`, and `execution`.
- Count-only writes accounting and static facts. Catalog-writing also writes WorkloadPlans and FaultScenarios.
- Dynamic enrichment remains an optional catalog-writing phase in this iteration. Adding input-only dynamic enrichment to count-only would be separate work.
- Static artifacts retain deterministic ordering and semantic identities. Dynamic artifacts record one observed run and may differ across repeated executions because their timestamps, threads, ordering, and runtime ids are evidence.
- Accounting stores the original `maxRecoverySchedulesPerVector`.
- On-demand generation uses that value by default but may override it. The WorkloadPlan id, fault vector, and effective cap identify the request; it has no separate id.
- Accounting preserves the original catalog-writing FaultScenario totals and separately maintains the current package totals. On-demand generation changes only the current totals. The manifest hashes change when package files change.
- Accounting uses JSON numbers, including for large combination counts.

## Accounting

### Configuration

- Keep `targetApplication`, `catalogWriteMode`, and `sagaSetSizes`.
- Replace `generationStrategy` plus `allowTypeOnlyFallback` with `sagaSetSelection`: `all`, `strict`, or `withTypeOnlyFallback`.
- Replace `inputPolicy` with `acceptedInputStatuses`, chosen from `fullyResolved`, `runtimeDependent`, `partial`, and `unresolved`.
- Rename `maxInputVariantsPerSaga` to `maxInputsPerSaga`.
- Rename `maxSchedulesPerInputTuple` to `maxStepSchedulesPerInputCombination`; omit it for serial scheduling.
- Rename `maxCatalogScenarios` to `maxWorkloadsWritten`; omit it for count-only.
- Remove `deterministicSeed`, which the current scheduler does not use.

### Metrics

- Remove `discovery`. `sagas` owns Saga totals and compact per-Saga rows; `inputs` owns the complete input funnel.
- Inputs satisfy `found = accepted + notAccepted` and `accepted = materializable + blocked`.
- Input blocker counts use `affectedInputsByReason` because one input may have several blockers.
- Interactions report direct step-interaction evidence and connected Saga-set counts for `strict` and `withTypeOnlyFallback`.
- Workloads retain `all`, `selected`, and `written`.
- Fault scenarios retain `vectorsComputed`, `possibleForComputedVectors`, and `written`.
- Per-Saga rows retain the FQN, step count, input counts, and strict/fallback direct-step-interaction counts.
- Repeated extraction-limit strings and warnings move to the detailed Saga facts.
- Remove `staticRecipeReady` from accounting and retain the materializability result used by generation.
- Remove top-level prose warnings. Anything that changes interpretation of the run becomes a named metric in its owning section.
- When dynamic enrichment runs, `dynamicEvidence` reports test outcomes, observations by kind, observations without test context, Saga-invocation attributions by status, unique input evidence, and WorkloadPlan participant evidence. Omit the section when enrichment did not run.
- Remove the undifferentiated dynamic `warningCount`. Ambiguous and unmatched attributions use candidate inputs or a short reason instead of repeated prose warnings; malformed runtime-input or test-run details remain in diagnostic output.
- Saga-invocation attribution counts use `exactInput`, `testAndShape`, `shapeOnly`, `ambiguous`, and `unmatched`. These names state how the match was made instead of using confidence labels.
- Unique inputs referenced by written WorkloadPlans are counted once under their strongest attribution: `exactInput`, then `testAndShape`, then `shapeOnly`; ambiguous candidates are not treated as observed inputs. Repeated runtime executions remain visible in the Saga-invocation attribution counts without inflating input coverage.
- WorkloadPlan participant evidence uses four exclusive counts: all inputs observed in one common test, all inputs observed but only across separate tests, some inputs observed, and no inputs observed. These counts do not claim that the persisted WorkloadPlan schedule ran.
- Add static event metrics for event emissions and resolved event routes. The retained catalog repeats 147 event consequences representing only 6 emission sites and 10 unique routes; count-only output currently loses this analysis entirely.

## Saga facts

- Keep one record per Saga with its FQN, steps, dependencies, command accesses, compensation information, and per-step analysis limitations.
- Combine each command dispatch with its aggregate-access result instead of repeating the same finding in separate `dispatches` and footprint arrays.
- Keep the conclusion about aggregate-key evidence, but do not expose temporary extractor assumptions such as a constructor-argument position.
- Remove `aggregateTypeName`, which the current adapter always writes as `null`; keep the aggregate name.
- Command repetition describes how often one command site may execute. Omit the default single execution and record only statically repeated or input-dependent command sites.
- Remove repeated per-record versions, the hashed Saga id, duplicated top-level limitations, and repeated type-only warning strings.
- Retain event routes under the Saga step that causes the emission. A route records the event, handler, processing method, functionality method, and downstream Saga once instead of embedding the same static chain in every WorkloadPlan.
- Decide the final step id and cross-file reference shape when auditing WorkloadPlans.

## Direct interaction facts

- A direct interaction remains binary because it describes two conflicting step accesses. Larger Saga sets are connected components/subsets derived from these binary relationships.
- Replace arbitrary `left` and `right` fields with a two-element `accesses` array. Each access retains its Saga, step, aggregate, access mode, and aggregate-key evidence.
- Retain one interaction `evidence` value. Remove the duplicated strict/fallback presence booleans, evidence kinds, conflict kinds, evidence list, and warnings; selection under `strict` or `withTypeOnlyFallback` is derived from the evidence.
- Keep one compact `id` because WorkloadPlans need to refer to the interactions that justify their Saga set. Do not embed the full interaction again in each WorkloadPlan.

## WorkloadPlans

- A WorkloadPlan records the participating Saga inputs and one concrete order of steps and event consequences.
- Keep one opaque WorkloadPlan `id`, which on-demand generation and execution use to select the plan.
- Participants use short plan-local ids such as `p1` and contain only the Saga FQN and input-fact id. Remove embedded accepted-input records, participant hashes, and repeated warnings.
- Use one ordered `schedule` instead of separate `forwardSchedule`, `eventConsequences`, and `normalSchedule` structures. Step occurrences use short plan-local ids such as `s1`; event occurrences reference their static Saga-fact route and triggering step occurrence.
- Put `faultSlot` directly on faultable scheduled steps. This preserves the mapping from a fault vector such as `101` to scheduled occurrences without repeating every step in a separate `faultSlots` array.
- Remove `kind`, which is derived from the participant count, and the invariant `executionShape`.
- Remove `compensationCheckpoints`. Saga facts own compensation knowledge; a FaultScenario owns the selected recovery order.
- Store only interaction ids in `interactions`; direct-interaction facts own the evidence details.
- Scheduled steps reference a participant plus a human-readable Saga-local step id such as `getUserStep#0`. Array order replaces explicit schedule-order fields, and repeated runtime step names and warnings are removed.
- Saga event routes use readable Saga-local ids such as `updateStudentNameStep#0/event#0`; WorkloadPlan event occurrences reference the route and their triggering step occurrence.
- Replace embedded `setupPlan` and `prerequisiteBaseline` objects with one optional `setup` id referencing `setups.jsonl`.

## Setups

- Add `setups.jsonl` because runtime setup is reusable across many WorkloadPlans and is currently repeated inside each one.
- A source-derived setup stores the ordered application calls extracted from the test and the input arguments bound to action results or properties. The automatic RemoveTournament–AddParticipant proof uses twelve actions and binds both Saga argument-1 values to the Tournament returned by action 12.
- A provider-backed setup stores the provider and required typed bindings once. The existing stale-read/event benchmark still uses this older manual mechanism.
- Source-derived setup currently replaces a manual provider only for the bounded path proven by v5. It does not yet make every Quizzes input combination executable or eliminate prepared application state; it automates how that state is reconstructed when the analyzer supports the test setup.
- Accounting must distinguish source-derived setups that are materializable or blocked and workloads that still rely on a configured provider. This lets the thesis measure progress from manual preparation toward automatic replay.
- Remove per-setup schema versions. Compact setup action arguments using the same kind-specific value representation chosen for input facts, and report blockers without repeating them at every nesting level.

## FaultScenarios

- A FaultScenario references one WorkloadPlan, assigns one fault vector, and fixes the complete action order for one recovery schedule.
- Keep the complete action order because compensations may interleave with remaining forward steps and event consequences. Different valid orders for the same WorkloadPlan and vector are different FaultScenarios.
- Use `workload`, `faultVector`, and `actions`. Each action is a one-key reference to a WorkloadPlan occurrence: `step`, `compensate`, or `event`.
- Keep one opaque scenario `id`, which execution uses to select the exact persisted scenario and which distinguishes recovery orders for the same WorkloadPlan and vector.
- Remove the record schema version and all per-action hashes, participant hashes, source fault-slot ids, compensation-checkpoint ids, event-consequence ids, and repeated occurrence ids.

## On-demand generation requests

- Keep one `requests.jsonl` inside each catalog-writing package. It is shared by all WorkloadPlans and vectors in that package; do not create one file per request or one file per WorkloadPlan.
- The command identifies the target package through its manifest path. A persisted request omits that path because its containing package already supplies the context.
- Persist one line per unique successful WorkloadPlan, fault-vector, and effective-cap request. An exact repeated request adds no duplicate row or FaultScenario. Failed requests return a terminal error without mutating the package.
- A request records its WorkloadPlan id, fault vector, effective recovery-schedule cap, uncapped possible recovery-schedule count, and the exact FaultScenario ids retained under that cap. Those first three fields identify the request without another opaque id.
- Catalog-writing accounting stores `faultScenarios.initial` and `faultScenarios.current`, each containing `vectorsComputed`, `possibleForComputedVectors`, and `written`. They begin equal. On-demand generation leaves `initial` unchanged and revises `current`.
- A successful new request updates `fault-scenarios.jsonl`, `requests.jsonl`, the current accounting totals, and their manifest hashes together.
- Historical pre-change behavior differed in three relevant ways: it stored the cap in the manifest, treated the optional CLI cap as a matching assertion rather than an override, wrote no request artifact, and replaced detailed per-vector rows directly inside accounting. The current implementation now follows the settled request ownership above.

## Dynamic observations and attribution

- When dynamic enrichment runs, retain one `dynamic-observations.jsonl` record for every actual runtime event. The current five event kinds become `stepStarted`, `stepFinished`, `commandSent`, `aggregateAccessed`, and `invariantViolation`.
- Every observation retains its id, sequence, timestamp, thread, compact test identity, and any known Saga, Saga invocation, step, and exact input id. Event-specific facts use `phase`, `outcome`/`error`, `command`, `access`, or `violation` instead of a generic payload.
- Retain command fields because they may contain several relevant aggregate ids even when the current copied command summary keeps only one root id.
- Remove the per-record schema, application name, duplicate test method/display name, duplicate functionality class/name variants, and `unitOfWorkVersion`. The current Saga-invocation id already includes that version and they agree throughout the retained Quizzes run.
- Keep timestamp, sequence, and thread because they can explain ordering, timing, and concurrency in later event and stale-read work.
- `dynamic-attribution-links.jsonl` records how one runtime Saga invocation maps to a static input. It identifies the test execution, Saga, Saga invocation, status, supporting observation ids, and either one matched input or ambiguous candidate inputs. Nothing currently needs to reference an attribution, so it has no separate id.
- Attribution statuses are `exactInput`, `testAndShape`, `shapeOnly`, `ambiguous`, and `unmatched`. `notCovered` is not an attribution record because no runtime Saga invocation exists; accounting derives unobserved inputs and WorkloadPlans by absence.
- The current `workload-dynamic-evidence.jsonl` is removed. The retained run copies 890,281 command/access objects from only 14,003 unique runtime events, and one 156-KB WorkloadPlan row pools 44 tests while matching only one of its two participant inputs.
- Remove the separate workload-dynamic-evidence manifest and join report. The main manifest indexes and hashes the two semantic dynamic files, and accounting owns their totals. Runtime input maps, Maven logs, and test reports remain working/diagnostic outputs outside the package.
- The raw simulator event stream is temporary input to normalization. Delete it after the normalized dynamic files and package manifest are finalized successfully; retain it when normalization fails so the failure can be inspected.

## Input facts

- Rename the source-resolution status `replayable` to `runtimeDependent`. In the retained Quizzes run, 880 inputs are currently called replayable but only 93 are materializable, so `replayable` incorrectly sounds like an execution result.
- Use `fullyResolved`, `runtimeDependent`, `partial`, and `unresolved` for source resolution. `materializable` remains the execution-readiness result.
- Keep acceptance and materialization as separate facts. Every input records `accepted` and `materializable`; `notAcceptedReason` appears only for a rejected input, and `blockers` appears only for a non-materializable input.
- Retain compact recipes for rejected inputs when extraction found one. A rejected input may still be materializable and may be accepted under another run configuration.
- Replace the redundant `inputRole` and `fixtureOrigin` pair with `testRole`: `featureUnderTest`, `setup`, or `setupHelper`.
- Replace the repeated source and owner fields with a compact `source` object containing `testClass`, `method`, optional `calledFrom`, `testRole`, and `call`. For setup inputs, retain the consuming test-method names as `usedBy`; omit it for a direct feature call where `source.method` already names the test.
- Rename `sourceMode` to `transactionModel`, with `saga`, `tcc`, or `unknown`. Remove `sourceModeConfidence`, `sourceModeEvidence`, repeated owner class names, the invariant owner `complete` flag, absent `sourceBindingName`, and prose `provenanceText`. The recipe owns structured argument provenance.
- Use compact, kind-specific argument recipes that omit fields irrelevant to that recipe kind.
- Store the Saga constructor arguments directly as `arguments`; remove the extra input-recipe wrapper, recipe version, recipe fingerprint, and recipe-level `executorReady`.
- Each argument retains its index, expected type, original source expression, and one kind-specific `value`. Literal, constructor, collection, runtime, property, transform, and unresolved values contain only the fields required for that kind.
- Constructor assignments retain the exact setter method and its value. This preserves reconstruction order and avoids carrying both a derived property name and the source method.
- Report materialization blockers once at the input level, with the affected argument, reason, and source expression. Do not repeat readiness, resolution, and blocker fields throughout nested recipe nodes.
- Runtime argument recipes retain the expected type, original source expression, provider, and scope. A Saga unit of work is participant-scoped; runtime beans such as `SagaUnitOfWorkService` and `CommandGateway` are execution-scoped.
- Any future sharing of one runtime value across multiple Saga participants must be expressed by the WorkloadPlan, where those participants are combined.
- Resolved 2026-09-02: `aggregateKeyEvidence` replaces `logicalKeyBindings` and represents `exact` or test/setup-scoped `sameSource` evidence. Equivalent getter/property expressions normalize to one source. All 884 input ids and non-evidence fields remained stable. Corrected semantic root paths intentionally changed evidence on 84 inputs: 83 values were replaced and one removed, reducing evidence-bearing inputs from 652 to 651. Discovery, acceptance, and materializability totals did not change; absent evidence remains absent rather than inferred.
- Strict multi-Saga input selection now requires positive exact or same-source evidence for each shared aggregate relationship. Missing evidence no longer passes merely because it is not contradictory. The all-space count remains the bounded Cartesian baseline.

## Things we want to revisit

### Aggregate keys and interaction confidence

- Resolved 2026-09-02: command aggregate roots are derived from constructor delegation to the framework `Command` constructor, then mapped at each call site. Literals, normalized getter chains, and Saga-constructor parameters retain semantic key evidence; explicit null roots and unsupported expressions stay keyless.
- `CreateQuestionCommand` and `CreateQuizCommand` illustrate the old third-argument error: both declare a null target root, while their third call arguments name a course or course execution. Those values are no longer recorded as Question or Quiz keys. `AnswerQuestionCommand`, `RemoveQuestionCommand`, and getter-based `UpdateQuestionCommand` calls now preserve their declared roots.
- Resolved 2026-09-02: strict conflict classification requires two-sided semantic-root evidence. The former `AnswerQuestionFunctionalitySagas::answerQuestionStep#2` versus `ConcludeQuizFunctionalitySagas::getQuizAnswerStep#0` case remains fallback-only because the latter command declares a null root.
- Fresh direct-interaction evidence is 0 exact, 535 symbolic, and 250 type-only, compared with the retained 0/152/612. Strict type-level connected sets increase, but strict accepted-input sets decrease after the positive input-evidence rule; neither direction alone is a success criterion.

### Static-analysis completeness

- Narrowed 2026-09-02: limitations now describe only unresolved command payloads or dispatches, rather than every unclassified helper call. Two of 134 Quizzes steps retain one limitation each: an unresolved `SagaCommand` payload and an unresolved dispatch through a helper `send` call.
- Resolved 2026-09-02: typed `SagaCommand` wrappers are transparent and do not duplicate forward accesses. The fresh artifact contains 132 forward command accesses.
- Resolved 2026-09-02: for the justified generic bare-`Command` compensation pattern, the aggregate is derived independently from the payload's service token through the matching command handler, and the root is derived from the payload's third argument under the base `Command` root-key contract. The realistic Quizzes proof records 26 compensation accesses; these remain internal compensation footprints and are not serialized as forward `commandAccesses`.

### Missing inputs and materialization

- Only 36 of 68 discovered Quizzes Sagas have accepted inputs. We want to determine how much comes from missing test coverage and how much comes from verifier extraction.
- Relative-date support improved the accepted-input result from 91 to 150 materializable inputs. The current full-cap result is therefore 150 materializable and 644 blocked out of 794 accepted inputs.
- `staticRecipeReady` can be false while `materializable` is true because the two checks treat runtime-owned arguments differently. We decided to remove `staticRecipeReady` and retain the materializability result that understands runtime-owned arguments.

### Dynamic attribution

- The verifier wrote `workloadPlanIds` in `dynamic-input-map.json`, while the simulator reader expected `scenarioPlanIds`. The simulator rejected the map, so the first 2026-09-02 bounded current-package smoke reported 0 exact-input, 2 test-and-shape, and 8 shape-only Saga-invocation attributions. The focused repair aligned the reader on `workloadPlanIds`; the equivalent rerun reports 2 exact-input, 0 test-and-shape, and 8 shape-only groups without changing matching rules.
- The old workload-shaped sidecar and `MATCHED_HIGH_CONFIDENCE` aggregation are removed. Current attribution is grouped by test execution and Saga invocation, and participant accounting distinguishes co-observation without claiming the persisted schedule ran.

### Running the system

- Catalog-writing generates the all-zero and single-fault vectors. Multi-fault vectors use on-demand generation.
- Execution takes an existing FaultScenario id, so trying vector `101` currently means running on-demand generation and then execution.
- After the artifact audit, we want a separate audit of commands, configuration, and whether these launches can be made simpler.
- A 2026-09-02 default catalog-writing run produced prerequisite WorkloadPlans whose four synthetic participant input ids were absent from `inputs.jsonl`; the current reader correctly rejected the first missing reference. The writer receives adapter inputs while prerequisite generation can create additional participant inputs. Diff inspection shows this ownership boundary predates the static-interaction correction, so it remains a separate catalog-writing follow-up; the count-only qualification is unaffected.

## After this artifact pass

Preflight, execution, impact, application benchmark results, and logs remain outside
this package-artifact change. The settled decisions now govern the related spec and
plan; confirmed analysis and workflow findings remain here until fresh qualification
shows which follow-up should be opened next.

Artifact sizes will be reported from realistic qualification runs for inspection, but
no arbitrary byte ceiling is an acceptance criterion. The contract prevents growth from
duplicated combination rows and copied event bodies through ownership and reference
rules.

## Final qualification ranking — 2026-09-02

The current-only artifact pass is complete. Fresh bounded evidence now qualifies the
corrected static interaction relationships, preflighted and replayed one source-derived
Remove/Add workload, persisted one multi-fault request, and normalized all five runtime
observation kinds.

Completed follow-up:

1. **Dynamic input-map plan-id mismatch repaired.** The equivalent bounded class now
   produces 2 exact-input groups instead of converting both to test-and-shape evidence.
2. **Static interaction correctness qualified.** Semantic command roots, positive strict
   input evidence, wrapper handling, compensation footprints, focused limitations, and
   size-1/2/3 accounting are now covered by fresh Quizzes evidence.

Ranking recorded at that qualification checkpoint (superseded by the canonical roadmap):

1. **Finish setup-result binding.** Setup translation and exact result references raised
   the single-input static result from 267 of 794 to 560 of 796. The remaining 236 inputs are
   now mostly partial or missing bindings rather than rejected setup descriptions.
2. **Define a broader impact contract.** The retained 19/15 Remove/Add final-state
   landscape still has flat ImpactV1 and remains downstream of reliable identity and
   executable input coverage.

## Findings recorded on 2026-09-04

These record the problems and missing measurements found after the artifact
reshape and workload-driven setup work. Consult the canonical roadmap for current status.

### 1. Exact event route identity — resolved 2026-09-04

A generated AnonymizeStudent event has two selected routes: one through
`QuizAnswerEventHandling` and one through `TournamentEventHandling`. Both classes expose
the method `handleAnonymizeStudentEvents`. The package used to discard the exact class,
so execution could not tell which route had been selected.

Saga facts now preserve the fully qualified `eventHandlingClass` and handler class for
each route. The reader restores them and the executor loads those exact classes; it no
longer searches globally by method or simple class name. Route ordering and ids remain
stable, and a negative test proves that a simple-name fallback is not accepted.

The same triple controls now pass the old ambiguous-class point. Four event-reaching
vectors instead report `SELECTED_SUBSCRIBER_NOT_FOUND`; the other two fault before the
event. This exposed the separate setup gap recorded in item 10.

### 2. Catalog-writing Cartesian scan — resolved 2026-09-04

The full-cap strict size-1–3 accounting result contains 74,273 selected input-bound
workloads, but catalog-writing previously constructed the complete Cartesian product and
applied strict selection only at each completed tuple. For Quizzes that meant visiting
1,247,308,000 input combinations before writing the selected workloads.

A full-cap writer was stopped after 8 minutes 42 seconds before publishing artifacts.
The reduced run with at most ten inputs per Saga completed and wrote 1,415 WorkloadPlans.

Tuple construction now applies conservative strict/fallback compatibility to each
partial tuple and abandons a branch only when no later Saga can connect it. Count-only
streams the surviving tuples instead of collecting them all. The strict size-1–3 result
completed in 99 seconds; size 1–4 completed in about 15 minutes while staying below
1 GiB in the Docker container. The all-space number remains an algebraic baseline and is
not enumerated.

### 3. Count-only workload setup accounting — resolved 2026-09-04

Count-only now partitions every selected strict/fallback workload into
`withSourceSetup`, `withoutSetup`, or `blocked`, including totals by Saga-set size. It
uses the same setup selection and materialization rules as catalog-writing and includes
the configured schedule count. Provider-backed workloads remain a separate configured
category rather than being mixed into input-derived counts.

The first strict size-1–3 measurement found 11,842 workloads with source setup, 93 that
needed no setup, and 62,338 blocked. After repairing setup translation and result
bindings, the same analysis finds 57,293 with source setup, one without setup, and
19,619 blocked out of 76,913. These are static setup results; runtime preflight remains
separate. The earlier size-4 result has not yet been rerun after this improvement.

### 4. Runtime setup evidence is still narrow

The earlier reduced writer attached source-derived setup to 408 workloads and all 94
referenced setup definitions passed static validation. After the setup repair, the same
bounded shape writes 1,620 workloads, of which 1,338 reference 311 source-derived setups.
The new setups have static validation but not broad runtime evidence. Runtime preflight
has exercised two representative workloads: the Remove/Add pair and one natural triple.
Both succeeded, covering five participants and all of their setup actions and bindings.

The complete preflight of the latest reduced package ran 402 candidate workloads. Every
worker reported `SETUP_READY`; 395 remain ready in the combined report, while seven are
changed to `FRESH_STATE_ISOLATION_FAILED` by the parent validator. Those seven did not
fail at runtime: their setup actions succeeded and their participants started. They have
zero setup-to-input bindings because the setup prepares application state without
supplying a Saga argument, while the parent currently requires every source setup to
have at least one binding.

Resolved parent bug: explicit empty binding arrays now pass when setup actions,
cleanup/baseline, materialization, startup, and isolation checks succeed; missing, null,
and malformed binding arrays remain rejected before deserialization. A fresh Docker
subset rerun of exactly the seven formerly rejected workloads passed 7/7 in 109.23 seconds
with unchanged package hashes (`verifiers/target/astra-qualification/state-only-preflight.json`).
The full historical report remains 395/402: this was a targeted regression rerun, not a
new complete preflight. The newer setup-translation package still needs its own runtime
qualification.

### 5. One fresh process per scenario is too slow for broad execution or search

Current Docker execution rebuilds or starts an isolated application process for each
FaultScenario. Recent runs took roughly 40–50 seconds each. At that rate, the reduced
package's 1,980 scenarios require roughly 22–28 hours, and even one scenario for each of
74,273 full-cap workloads would require several weeks.

This is not yet a reason to weaken isolation: a fresh process and H2 database currently
provide the reset boundary that makes attempts comparable. Before GA work, investigate a
simple batch runner that avoids repeated build/start overhead while retaining an honest
fresh-state boundary, or explicitly budget a smaller representative execution set.

### 6. ImpactV1 still does not provide a dependable search objective

In the fresh twelve-scenario smoke, the Remove/Add pair produced one ImpactV1 finding in
the all-zero scenario and zero in each single-fault scenario. Exact route identity now
works for the triple, but four event-reaching scenarios cannot be evaluated because the
selected subscriber's state is absent. This is useful execution evidence, but not yet a
sensible optimization landscape.

Next measurement: after subscriber setup is available, rerun the same controls and
inspect why the all-zero pair reports an invariant violation. Broaden impact only after
distinguishing an executor/control problem from a genuine limitation of ImpactV1.

### 7. Size 4 accounting — measured 2026-09-04

Strict size 4 adds 37 connected Saga sets and 321,120 selected input-bound workloads.
Only 4,800 have source setup and none are ready without setup; 316,320 are blocked. The
ready share is therefore about 1.5%, so a full size-4 catalog is poor value until setup
and input coverage improve.

### 8. Half of the discovered Sagas still have no accepted input

Quizzes currently has 68 discovered Sagas, but only 36 have at least one accepted input.
This is different from the 643 recipe-blocked accepted inputs: these 32 Sagas never reach the
accepted-input pool at all.

Next investigation: classify each missing Saga as absent from relevant tests, present in
tests but missed by input extraction, or deliberately excluded by the accepted-status
configuration. Fix representative verifier gaps; report genuine missing test coverage as
an application limitation instead of manufacturing inputs.

### 9. Launch commands and configuration still need a separate cleanup

Generation, on-demand generation, preflight, execution, and impact currently expose
overlapping environment variables and command shapes whose ownership is difficult to
understand. We agreed to audit this separately after the artifact shape. Do it after the
next correctness and measurement work so it documents the launcher we actually keep,
not the one we are about to change.

### 10. Event-expanded workloads lack subscriber setup

Exact route identity now reaches the selected Quizzes event consumer correctly, but four
triple controls stop because no eligible subscriber exists. Their source setup creates a
CourseExecution and User; the selected AnonymizeStudent routes require an existing
QuizAnswer or Tournament subscriber. The current setup belongs to the three selected
Saga inputs and does not add state required only by the chosen event consequence.

Before broad event execution, decide the smallest way for an event-expanded workload to
carry the selected consumer's prerequisites. Count-only setup accounting currently
measures the base input workload, so it must not be presented as proof that every event
expansion is executable.

### 11. Accepted inputs often lost their usable test setup — improved 2026-09-04

A diagnostic all-single-Saga catalog produced 794 base input workloads: 267 were ready
and 527 were blocked. Every input with an attached source setup was ready; all 527
blocked inputs had no setup attached. Their first blockers were 372
`callReceiverNotReady`, 81 `propertyReceiverNotReady`, 39 `unmaterializableReceiver`,
15 `unresolvedPlaceholder`, 13 `eventPayloadPlaceholder`, four loop-dependent
mutations, and three unknown values.

The largest call-receiver clusters are CreateQuestion (85), CreateTopic (83), AddStudent
(62), FindTournament (47), AddParticipant (23), and GetCourseExecutionById (22). A
follow-up measurement split the same 794 inputs into 174 with attached setup, 93 that
need no setup, and 527 whose test class has setup but receives none. The 527 failures
divide into 298 rejected setup plans, 206 inputs for which no setup-result binding was
found, and 23 with only some required arguments bound. No input matched several setup
plans, and no blocked input came from a test class without setup.

The rejected plans largely share a few translation problems rather than 298 independent
causes. All 298 include `UNSUPPORTED_LOCAL_DATE_EXPRESSION` and
`INCOMPATIBLE_SETUP_LITERAL`; the former is misleading because the mapper also sends
non-date runtime expressions such as `Arrays.asList(topicDto)` through its date-only
fallback. Twenty also contain an unsupported constructor and twelve an unresolved
property. Fix setup value translation first, then connect setup action results to the
remaining participant arguments and repeat this measurement.

That repair now recognizes the observed `Arrays.asList(...)` shape, bounded constant
string concatenation, and `QuizDto`, and preserves the exact source occurrence of a
facade result when binding setup actions to inputs. The repeated measurement changes
the accepted inputs from 174 with setup / 93 without setup / 527 blocked out of 794 to
559 with setup / one without setup / 236 blocked out of 796. The remaining 236 divide
into three rejected plans, 175 partial bindings, and 58 with no binding. No multiple-setup ambiguity was
found. The largest remaining problem is therefore no longer translating the setup; it
is connecting every setup result needed by one input, especially calls made in the test
method before the target Saga call. Three rejected plans also need a two-property result
path such as `quiz.aggregateId`, while the current binding stores one property.

The bounded nested-property repair subsequently resolved those three rejected plans:
the unchanged 796 accepted singles now divide into 562 with setup, one without setup,
and 233 blocked. All three newly covered FindQuiz inputs passed selected Docker preflight
through the real parent/worker boundary. The exact `quiz.aggregateId` path reuses the
existing property string with closed pre-dispatch getter/type checks. These inspection
inputs establish reconstruction/startup coverage only; whole-test replay and harmful
behavior are not implied. Partial/missing feature-local bindings remain the next slice
in `issues/2026-09-04-complete-source-derived-setup/`.

The completed feature-prefix slice preserves same-feature action order, exact target
cutoffs, and pre-call DTO mutations. Final ordinary single-input coverage is 576 with
source setup, one without setup, and 219 blocked: 577/796 static candidates. It adds 17
feature-prefix inputs after the nested-path gain of three, while removing three old
CreateQuiz false positives whose DTO mutations expose unresolved question references.
This replaces the intermediate 580 count. Remaining runtime method-authority and
failed-worker diagnostic limits are recorded in the new issue's `RUNTIME-FOLLOWUPS.md`.
