# Current-only verifier artifacts

## What current-only verifier artifacts are

The verifier currently stores useful analysis, generation, and runtime evidence in a
set of overlapping reports. Numerical accounting contains thousands of concrete Saga
sets, WorkloadPlans repeat inputs and setup, and optional dynamic evidence copies the
same runtime events into many workload-shaped records. This makes it difficult to tell
what the verifier actually discovered, what it can materialize, and what has runtime
support.

This change gives each fact one natural owner. Accounting becomes a compact numerical
account of one run. Detailed static facts, executable plans, requested fault scenarios,
and runtime observations remain inspectable without being copied into unrelated
records. The result is one current package that supports thesis evaluation and the
existing bounded replay path without retaining old package contracts.

## Goals

1. Make accounting sufficient for understanding the run through configuration,
   metrics, and compact per-Saga diagnostics.
2. Store detailed Saga, input, interaction, setup, workload, fault, request, and
   dynamic facts once at the level where they occur.
3. Make every cross-file relationship explicit and readable.
4. Preserve the current static counts, generation semantics, fault semantics, and
   supported replay behavior while changing artifact ownership.
5. Make optional dynamic evidence state exactly what was observed and how it was
   associated with static inputs.
6. Keep the design appropriate for a local single-student thesis tool: JSON/JSONL,
   deterministic static output, bounded catalogs, and no database or migration layer.

## Non-goals

- Improving aggregate-key extraction, including the current command-argument
  assumption.
- Changing strict interaction classification, input extraction, materializability,
  setup inference, schedule generation, fault semantics, execution, impact analysis,
  or genetic search.
- Fixing the current runtime input-map integration mismatch.
- Adding count-only dynamic enrichment, a query language, database, dashboard, HTML
  report, or inspection CLI.
- Preserving or migrating v4, v5, or other historical package readers.
- Treating Maven logs, test reports, parser traces, preflight reports, execution
  reports, ImpactV1 reports, or application benchmarks as package facts.

## Functional requirements

### Package entry point and lifecycle

- **FR-1:** Every run produces one manifest with one integer `formatVersion` and a
  `files` object keyed by artifact role. Each present file entry contains only its path
  and SHA-256 hash.
- **FR-2:** The package format version governs the complete package. Individual files
  and JSONL records do not repeat schema or format versions.
- **FR-3:** Count-only writes accounting plus Saga, input, and direct-interaction facts.
  It writes no placeholder setup, workload, fault-scenario, request, or dynamic files.
- **FR-4:** Catalog-writing additionally writes setups, WorkloadPlans,
  FaultScenarios, and one package-level on-demand request stream.
- **FR-5:** Optional dynamic enrichment remains part of catalog-writing and adds
  runtime observations and attribution links before the run is finalized. The raw
  simulator event stream is temporary input: delete it after successful normalization,
  but retain it when normalization fails so the failure can be inspected.
- **FR-6:** Present files and references must remain inside the manifest directory.
  Readers validate required files, hashes, unique identities, and cross-file
  references before returning selected executable content.
- **FR-7:** The reader supports only the current package format. `formatVersion` and
  the required current fields determine acceptance; the reader maintains no blacklist
  of removed field names. Historical packages are regenerated when current execution
  evidence is needed.

### Accounting

- **FR-8:** Accounting contains only configuration needed to interpret the run,
  aggregate metrics, and compact per-Saga rows. It contains no row per Saga set,
  WorkloadPlan, fault vector, recovery schedule, or runtime event.
- **FR-9:** Configuration uses `targetApplication`, `catalogWriteMode`,
  `sagaSetSizes`, `sagaSetSelection`, `acceptedInputStatuses`, and the applicable
  input, step-schedule, workload, and recovery-schedule caps. Inapplicable caps are
  omitted.
- **FR-10:** Input metrics satisfy `found = accepted + notAccepted` and
  `accepted = materializable + blocked`. Rejection and blocker reasons are counted
  without storing the affected input rows in accounting.
- **FR-11:** Saga metrics include totals and one compact row per Saga with its FQN,
  step count, input counts, and strict/fallback direct-interaction counts.
- **FR-12:** Interaction metrics report direct evidence and connected Saga-set counts
  under `strict` and `withTypeOnlyFallback`. They do not persist the connected sets.
- **FR-13:** Workload metrics distinguish all possible workloads under the configured
  bounds, the workloads selected by the Saga-set rule, and the records written.
- **FR-14:** Setup metrics distinguish source-derived setups that are materializable or
  blocked and workloads that rely on a configured provider.
- **FR-15:** FaultScenario metrics preserve `initial` catalog-writing totals and
  maintain `current` package totals. Each contains computed vectors, possible recovery
  schedules for those vectors, and records written.
- **FR-16:** When dynamic enrichment runs, accounting reports test outcomes,
  observations by kind, observations without test context, Saga-invocation
  attributions by status, unique input evidence, and WorkloadPlan participant evidence.
  The complete section is omitted otherwise.
- **FR-17:** Large combinatorial totals are JSON numbers with exact integer values.

### Static facts

- **FR-18:** Each discovered Saga has one record identified by FQN. It contains its
  dependencies, human-readable Saga-local step ids, command/aggregate accesses,
  compensation knowledge, event routes, and concise per-step analysis limitations.
- **FR-19:** A command access combines dispatch and aggregate-access evidence. It keeps
  the aggregate, access mode, key conclusion, and non-default repetition without
  exposing temporary extractor assumptions.
- **FR-20:** Event routes belong to the triggering Saga step and use Saga-local ids.
  A route records the event-to-handler-to-downstream-Saga chain once.
- **FR-21:** Every extracted input has one record with its id, Saga FQN, source,
  transaction model, test role, constructor arguments, acceptance, materializability,
  and applicable reason or blockers.
- **FR-22:** Input source resolution uses `fullyResolved`, `runtimeDependent`,
  `partial`, or `unresolved`. Acceptance and materializability remain independent.
- **FR-23:** Input argument recipes use compact kind-specific values. Runtime-owned
  values retain their provider and scope; blockers occur once at input level.
- **FR-24:** Input aggregate-key evidence can represent exact values and canonical
  same-source provenance. The artifact omits evidence the analyzer does not currently
  produce.
- **FR-25:** Each direct interaction has one compact id and a two-element `accesses`
  array. Each access identifies its Saga, step, aggregate, mode, and key evidence; the
  interaction retains one evidence value.

### Executable catalog

- **FR-26:** Each reusable setup has one record. Source-derived setups contain ordered
  application calls and bindings to earlier results; provider-backed setups contain
  the provider and required typed bindings.
- **FR-27:** Each WorkloadPlan references its participant inputs, optional setup, and
  direct interactions. It owns one ordered schedule of step and event occurrences.
- **FR-28:** WorkloadPlan participants and occurrences use short plan-local ids.
  Scheduled steps reference one participant and one Saga-local step and carry their
  fault slot when faultable. Scheduled events reference one Saga event route and their
  triggering step occurrence.
- **FR-29:** Each FaultScenario references one WorkloadPlan, assigns one binary fault
  vector, and stores one complete action order. Actions reference WorkloadPlan step or
  event occurrences; compensation references the forward step occurrence it undoes.
- **FR-30:** One package-level request stream records each unique successful on-demand
  WorkloadPlan, vector, and effective-cap request. A record includes the uncapped
  possible recovery count and the exact FaultScenario ids retained. The WorkloadPlan,
  vector, and effective cap identify the request; it has no separate id.
- **FR-31:** Repeating the same on-demand request adds no duplicate request or
  FaultScenario. A failed request reports an error without mutating the package.
- **FR-32:** On-demand generation uses the catalog-writing recovery cap from accounting
  unless the request supplies an override. A successful request updates the request
  stream, fault catalog, current accounting totals, and manifest hashes together while
  preserving initial accounting totals.

### Dynamic evidence

- **FR-33:** Each runtime event is stored once as an observation with its id, kind,
  sequence, timestamp, thread, compact test identity, and any known Saga, Saga
  invocation, step, or exact input id.
- **FR-34:** Observation kinds are `stepStarted`, `stepFinished`, `commandSent`,
  `aggregateAccessed`, and `invariantViolation`. Kind-specific facts are stored under
  clear phase, outcome/error, command, access, or violation fields.
- **FR-35:** Command observations retain captured command fields because several
  aggregate identities may be relevant even when one root id exists.
- **FR-36:** Each attribution record groups one runtime Saga invocation within one test
  execution and references its supporting observations. It records its Saga and one of
  `exactInput`, `testAndShape`, `shapeOnly`, `ambiguous`, or `unmatched`.
- **FR-37:** A successful attribution identifies one matched input. An ambiguous
  attribution identifies its candidate inputs. An unmatched attribution may contain a
  short reason. Repeated prose warnings are not package facts.
- **FR-38:** Unique input evidence counts each input once under its strongest
  attribution: `exactInput`, then `testAndShape`, then `shapeOnly`. Ambiguous candidates
  do not count as observed inputs.
- **FR-39:** WorkloadPlan participant evidence distinguishes all inputs observed in one
  common test, all inputs observed only across separate tests, some inputs observed,
  and no inputs observed. Only `exactInput` and `testAndShape` establish co-observation;
  no category claims the exact WorkloadPlan schedule ran.

### Outputs outside the package

- **FR-40:** Setup preflight, execution, ImpactV1, application benchmark, and diagnostic
  outputs remain separate. They reference the package, WorkloadPlan, FaultScenario, or
  attempt identities needed to relate their result back to generation.

## Architecture

The verifier retains its existing analysis and generation boundaries:

```text
source and test analysis
  -> Saga, input, interaction, and event-route facts
  -> accounting
  -> optional setup and WorkloadPlan generation
  -> optional eager FaultScenario generation

optional catalog-writing dynamic phase
  -> runtime observations
  -> Saga-invocation/input attributions
  -> dynamic accounting metrics

later on-demand generation
  -> request + additional FaultScenarios + current totals

later preflight/execution
  -> separate attempt reports
```

One package reader owns manifest, integrity, and reference validation. Selecting one
FaultScenario may scan bounded JSONL streams; this iteration adds no persistent index or
database. Static artifacts use stable ordering and semantic identities. Dynamic
artifacts faithfully record one runtime execution and may vary across repeated runs.

## Data model

- **Manifest:** current format version plus a role-keyed map of paths and hashes.
- **Accounting:** configuration and numerical results for generation, plus preserved
  initial and maintained current fault totals.
- **Saga fact:** one Saga and its steps, accesses, compensations, routes, and analysis
  limitations.
- **Input fact:** one test-derived Saga input, its recipe, acceptance, and
  materializability.
- **Direct interaction:** one binary relationship between two Saga-step accesses.
- **Setup:** reusable source-derived or provider-backed preparation.
- **WorkloadPlan:** selected participant inputs and one normal step/event schedule.
- **FaultScenario:** one WorkloadPlan, vector, and complete recovery-aware action order.
- **Request:** one successful on-demand WorkloadPlan/vector/cap result.
- **Dynamic observation:** one event that actually occurred during a test run.
- **Dynamic attribution:** one runtime Saga invocation associated with a static input or
  candidate inputs.

The principal relationships are:

```text
Input -> Saga
Interaction -> two Saga steps
Setup <- WorkloadPlan -> Inputs + Interactions + Saga steps/routes
FaultScenario -> WorkloadPlan occurrences
Request -> WorkloadPlan + FaultScenarios
Attribution -> Observations + Input
```

Saga steps and routes use human-readable Saga-local ids. WorkloadPlan participants and
scheduled occurrences use short plan-local ids. Opaque ids remain only where later
commands or cross-file records must select an entity; requests are identified by their
WorkloadPlan, vector, and effective cap.

## Security model

Not affected. The verifier continues to process local source, tests, generated
packages, and runtime evidence; readers reject invalid paths, hashes, identities, and
references rather than guessing.

## Operating

Count-only and catalog-writing remain the supported generation modes. Configuration
continues to bound input variants, step schedules, written workloads, and recovery
schedules. Dynamic enrichment remains optional within catalog-writing. On-demand
generation and execution continue to select a package through its manifest and select
the requested WorkloadPlan or FaultScenario by id.

The implementation stays concrete and local to the existing verifier pipeline. This
change adds no generic artifact framework, compatibility layer, persistent index,
configurable validation modes, or unrelated audit fixes.

The format is current-only. Rollback is reverting the implementation and regenerating
the required package; there is no migration or production rollout. Execution updates
the affected canonical verifier documentation after the new outputs and commands have
been verified.

## Future roadmap

Confirmed audit findings remain separate work: aggregate-key and strict-interaction
correctness, command-wrapper and compensation analysis, meaningful completeness
diagnostics, missing-input and materialization coverage, same-source input identity,
the runtime input-map mismatch, command/configuration simplification, impact analysis,
and genetic search. After a fresh baseline, only findings that threaten evaluation
validity or unlock required experiments should delay the wider thesis work.

## Open decisions

None.
