# Trustworthy static Saga interactions

## What trustworthy static Saga interactions are

The verifier uses command and aggregate-access evidence to decide which Saga steps may
touch the same aggregate instance. Those direct interactions are then used to count and
select Saga sets. Today, aggregate-key extraction assumes that the third argument at a
command creation site is the key, `strict` can accept a symbolic access paired with an
unknown key, and ordinary helper calls make every Quizzes step appear incompletely
analyzed. The resulting interaction counts are useful as an upper-bound exploration,
but are not yet strong enough to present as a trustworthy strict thesis metric.

This change makes key extraction follow the command's actual root-aggregate identity,
keeps uncertain accesses in the fallback view, understands the existing `SagaCommand`
wrapper and generic compensation pattern, and reports only meaningful analysis
limitations. It deliberately does not increase input materializability or runtime
coverage.

## Goals

1. Determine a dispatched command's aggregate-key expression from its command
   definition instead of a fixed creation-site argument position.
2. Make `strict` require positive evidence that the relevant Saga inputs can refer to
   the same aggregate key.
3. Keep possible interactions with missing key evidence visible under
   `withTypeOnlyFallback`.
4. Recognize typed commands through `SagaCommand` wrappers and the existing generic
   semantic-lock compensation pattern without duplicating accesses.
5. Replace the current all-false completeness result with concise limitations that
   identify unresolved command or aggregate-access analysis.
6. Recalculate the size-1–3 Quizzes interaction and workload-selection metrics from the
   corrected evidence and explain the changes with concrete examples.

## Non-goals

- Increasing accepted-input coverage or the current 91/794 materializable-input count.
- Adding new input recipe kinds, setup inference, providers, preflight support, or
  execution behavior.
- Changing step scheduling, fault vectors, recovery schedules, dynamic attribution,
  ImpactV1, or genetic search.
- Building a general Java data-flow engine. Unsupported command-constructor flows stay
  uncertain rather than being guessed.
- Preserving generated package identities or compatibility with packages created
  before this correction. Current packages are regenerated when needed.

## Functional requirements

### Aggregate-key extraction

- **FR-1:** For an application command extending `Command`, the verifier determines
  which public constructor input supplies `Command.rootAggregateId` by tracing the
  command constructor's explicit superclass or constructor delegation.
- **FR-2:** The supported trace includes a constructor parameter passed directly as the
  root id and a direct property/getter expression on a constructor parameter. A direct
  `new Command(unitOfWork, serviceName, aggregateId)` continues to use the base
  `Command` constructor contract.
- **FR-3:** A literal root id is exact evidence. A successfully traced non-literal root
  id retains a stable symbolic origin that can be related to a Saga constructor input
  and its extracted test provenance.
- **FR-4:** A `null` root id, ambiguous constructor path, unsupported transformation,
  or unresolved symbol produces no invented key. The access remains type-only when its
  aggregate and access mode are otherwise known.
- **FR-5:** Other IDs carried by a command do not become the aggregate key unless they
  flow to `Command.rootAggregateId`.

### Interaction selection

- **FR-6:** A direct interaction still requires matching aggregate identity, two
  different Sagas, and at least one write access.
- **FR-7:** Exact key values match only when equal and do not match when both are known
  to differ.
- **FR-8:** A symbolic direct-interaction candidate requires a resolved root-key path
  on both accesses. At input-bound selection, `strict` retains that candidate only
  when the selected inputs provide positive equality evidence: the same exact value or
  the same canonical test/source origin. Matching aggregate type or similarly named
  variables alone is insufficient.
- **FR-9:** Any relationship involving a type-only or otherwise unresolved key is
  excluded from `strict` and remains eligible for `withTypeOnlyFallback`.
- **FR-10:** The bounded `all` workload count remains the Cartesian input-backed
  baseline. It does not require interaction or shared-key evidence.
- **FR-11:** Direct interaction facts continue to use the existing human-facing
  evidence names `exact`, `symbolic`, and `typeOnly`; this change adds no new artifact
  enum merely to expose extractor internals.
- **FR-12:** In accounting, `strict.connectedBySize` counts Saga sets connected by
  exact or two-sided symbolic direct-interaction candidates.
  `strict.withAcceptedInputsBySize` counts only those sets for which at least one
  accepted input tuple positively satisfies the relevant key relationships. Selected
  input-bound workload totals use the same positive tuple rule. A symbolic candidate
  without a positively matching input remains inspectable in `interactions.jsonl` but
  does not contribute a strict selected workload.

### Wrappers, compensation, and limitations

- **FR-13:** A typed application command wrapped in `SagaCommand` is recorded once as
  the dispatched application access. The wrapper and `commandGateway.send(...)` do not
  create duplicate accesses or unresolved-call limitations.
- **FR-14:** For the existing generic compensation pattern, the verifier derives the
  target service and root aggregate key from the bare `Command` payload when those
  arguments are statically available. Otherwise it records one concise unresolved
  compensation limitation.
- **FR-15:** Getter, setter, collection, wrapper-configuration, and recognized gateway
  calls do not by themselves make step dispatch analysis incomplete.
- **FR-16:** A step reports an analysis limitation only when an unsupported shape may
  hide or misidentify a command dispatch, aggregate target, aggregate key, or
  compensation access.
- **FR-17:** Limitation records remain short and grouped with the affected Saga step.
  Temporary implementation assumptions such as a constructor argument number are not
  written into package artifacts.

### Evaluation and stability

- **FR-18:** The same bounded size-1–3 Quizzes count-only configuration is run before
  and after the correction. The comparison includes direct interaction evidence,
  connected Saga sets, input-backed selected workloads, and representative changed
  relationships.
- **FR-19:** No acceptance target is attached to higher or lower interaction counts.
  The result is accepted when every changed classification is explained by the new
  evidence rules and the accounting equations still reconcile.
- **FR-20:** Input discovery, acceptance, rejection, and materializability totals remain
  unchanged unless implementation discovery proves that one is directly and correctly
  dependent on the repaired root-key trace. Such a dependency is reported before its
  result is absorbed.
- **FR-21:** Output ordering and identities remain deterministic for identical source,
  configuration, and analyzer behavior.

## Architecture

The correction stays inside the existing verifier flow:

```text
command definitions and handlers
  -> command root-key path
  -> Saga-step command access
  -> Saga-input/test key origin
  -> strict or fallback interaction selection
  -> accounting and static artifact output
```

Command-definition analysis owns the semantic root-key path. Saga-step analysis applies
that path at each command creation and follows recognized wrappers. Input analysis owns
concrete and same-source test evidence. The interaction selector combines these facts;
writers only serialize the resulting conclusions.

The implementation extends the concrete visitors, analysis state, adapter, and
interaction selector already used by the verifier. It adds no generic analysis
framework, database, compatibility reader, or application-specific Quizzes rule.

## Data model

- **Command root-key path:** the constructor input, optionally followed by a direct
  property/getter, that supplies `Command.rootAggregateId`; absent when unresolved or
  intentionally `null`.
- **Step access:** the existing command, aggregate, mode, phase, multiplicity, and key
  conclusion, now derived from the semantic root-key path.
- **Input key evidence:** the existing exact value or canonical same-source origin used
  to compare selected Saga inputs.
- **Direct interaction:** the existing binary relationship between two step accesses.
  Larger Saga sets continue to be derived from these relationships.
- **Analysis limitation:** one concise reason why a step's command or aggregate-access
  conclusion may be incomplete.

No new package file or top-level artifact role is introduced.

## Security model

Not affected. The verifier continues to analyze local source and tests without adding
new execution or external input boundaries.

## Operating

The existing `strict`, `withTypeOnlyFallback`, and `all` selections remain available.
Users regenerate packages after the correction because interaction and dependent plan
identities may legitimately change. Rollback is reverting the implementation and
regenerating the package; there is no migration or production rollout.

The realistic qualification is count-only. It does not require Docker, dynamic
enrichment, preflight, or scenario execution.

## Future roadmap

After these metrics are trustworthy, accepted-input materializability should be handled
as a separate effort, one recurring blocker family at a time. Broader impact analysis
and search remain downstream of reliable selection and executable coverage.

## Open decisions

None.
