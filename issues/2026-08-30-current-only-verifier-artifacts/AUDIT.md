# Current-only verifier artifact audit

Working notes from our artifact-by-artifact review. These will become the basis
for the short spec and plan after we finish the audit.

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
- All 884 retained Quizzes inputs currently have empty `logicalKeyBindings`. Input-tuple compatibility therefore cannot reject combinations whose known aggregate ids disagree; after a Saga set is selected, its inputs are treated as mutually compatible and counted as a Cartesian product.
- Investigate input-key evidence at two levels: exact values when a test contains one, and canonical same-origin provenance when runtime-generated ids are read from the same DTO or setup result. The existing dummyapp fixture demonstrates exact bindings such as `orderId: 13`; realistic Quizzes tests commonly expose symbolic sameness such as two Saga inputs both reading `tournamentDto.aggregateId`. The current Quizzes analysis records neither form.
- Replace the exact-value-only `logicalKeyBindings` map with input aggregate-key evidence that can represent `exact` and `sameSource`. Same-source evidence must be scoped to its test/setup origin and normalize equivalent expressions such as `tournamentDto.aggregateId` and `tournamentDto.getAggregateId()`.
- The package must not infer or invent this evidence. Until the analyzer learns to produce it, Quizzes input facts will omit aggregate-key evidence and input combinations will remain unfiltered by concrete or same-origin ids.
- Keep artifact reshaping separate from the static-analysis improvement if necessary: the clean artifact can support the evidence first, while a later focused change populates it. Evaluation results must continue to report that the evidence is absent.
- The current tuple joiner is only a contradiction filter: it rejects shared exact keys with different values, but missing or unrelated bindings pass. A future input-related evaluation must require positive `exact` or `sameSource` evidence; the brute-force evaluation continues to allow the Cartesian product.

## Things we want to revisit

### Aggregate keys and interaction confidence

- Aggregate-key extraction currently assumes that the third command-constructor argument is the target aggregate key. This fails for commands whose key is elsewhere, create commands without an existing id, and commands carrying another aggregate's id.
- Plain variables often become type-only evidence even when source or test flow may contain a usable value.
- Current `strict` matching accepts a symbolic-plus-type-only relationship as symbolic. A symbolic key on one side cannot prove equality when the other key is unknown.
- The concrete example is `AnswerQuestionFunctionalitySagas::answerQuestionStep#2` versus `ConcludeQuizFunctionalitySagas::getQuizAnswerStep#0`.
- In the retained Quizzes run, the current classifier reports 0 exact, 152 symbolic, and 612 type-only direct step interactions. The symbolic count includes the questionable mixed-confidence case.

### Static-analysis completeness

- All 134 Quizzes Saga steps currently report `forwardAnalysisComplete: false`.
- The visitor marks a step incomplete for every method call it cannot classify, including ordinary getters, setters, collection operations, and framework calls such as `getServiceName`, `stream`, `toList`, and `equals`.
- This makes the current completeness flag too broad to show which steps may actually be missing command or aggregate-access information. We want to inspect this analysis separately and replace it with evidence that has a clear, useful meaning.
- Application commands extend `Command`, and Saga execution wraps them in `SagaCommand` to carry forbidden states and semantic locks. The visitor already recognizes many typed payload commands but does not follow the wrapper variable into `commandGateway.send(...)`, so it adds misleading unresolved-call diagnostics around an access it has already found.
- Some explicit compensations intentionally wrap a bare `Command`. That payload carries the service, aggregate id, and unit of work; the Saga handler uses the surrounding `SagaCommand` to restore the semantic lock without an application-specific domain command. The current visitor recognizes that compensation exists but usually cannot describe its aggregate access.
- Improvements to investigate: follow `SagaCommand` back to its payload, recognize the generic semantic-lock compensation pattern, relate it to the known access of the same step when justified, and stop treating clearly harmless helper calls as missing command analysis.

### Missing inputs and materialization

- Only 36 of 68 discovered Quizzes Sagas have accepted inputs. We want to determine how much comes from missing test coverage and how much comes from verifier extraction.
- Only 91 of 794 accepted inputs are materializable. This is the main current bottleneck.
- `staticRecipeReady` can be false while `materializable` is true because the two checks treat runtime-owned arguments differently. We decided to remove `staticRecipeReady` and retain the materializability result that understands runtime-owned arguments.

### Dynamic attribution

- The verifier wrote `workloadPlanIds` in `dynamic-input-map.json`, while the simulator reader expected `scenarioPlanIds`. The simulator rejected the map, so the first 2026-09-02 bounded current-package smoke reported 0 exact-input, 2 test-and-shape, and 8 shape-only Saga-invocation attributions. The focused repair aligned the reader on `workloadPlanIds`; the equivalent rerun reports 2 exact-input, 0 test-and-shape, and 8 shape-only groups without changing matching rules.
- The old workload-shaped sidecar and `MATCHED_HIGH_CONFIDENCE` aggregation are removed. Current attribution is grouped by test execution and Saga invocation, and participant accounting distinguishes co-observation without claiming the persisted schedule ran.

### Running the system

- Catalog-writing generates the all-zero and single-fault vectors. Multi-fault vectors use on-demand generation.
- Execution takes an existing FaultScenario id, so trying vector `101` currently means running on-demand generation and then execution.
- After the artifact audit, we want a separate audit of commands, configuration, and whether these launches can be made simpler.

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

The current-only artifact pass is complete. Fresh bounded evidence preserved the retained
interaction topology, preflighted and replayed one source-derived Remove/Add workload,
persisted one multi-fault request, and normalized all five runtime observation kinds.

Completed follow-up:

1. **Dynamic input-map plan-id mismatch repaired.** The equivalent bounded class now
   produces 2 exact-input groups instead of converting both to test-and-shape evidence.

Ranked remaining follow-up by evaluation validity and executable coverage:

1. **Improve accepted-input materializability.** Only 91 of 794 accepted Quizzes inputs
   are materializable; address one representative blocker family at a time after exact
   runtime identity is trustworthy.
2. **Correct aggregate-key extraction and interaction confidence.** The third-argument
   assumption and 0/152/612 exact/symbolic/type-only split remain important for static
   evaluation, but they do not block the already qualified current package lifecycle.
3. **Define a broader impact contract.** The retained 19/15 Remove/Add final-state
   landscape still has flat ImpactV1 and remains downstream of reliable identity and
   executable input coverage.

This ranking does not authorize or pre-specify any follow-up implementation.
