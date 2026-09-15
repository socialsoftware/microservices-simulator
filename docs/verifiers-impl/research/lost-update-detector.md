# Lost-update detector: working brief

This records the design following the [stale-write experiment](../evidence/stale-write-2026-09-15/README.md)
and the [automatic-copy proof](../evidence/inferred-stale-write-2026-09-15/README.md).
The approved [integration contract](../../../issues/2026-09-15-lost-copied-update-integration/SPEC.md)
owns the implemented slice; this design history does not approve an unbounded data-flow tracer.
The user wants normal writes and compensation covered, then stronger GA comparisons and
paper updates. Cross-workload RL should follow a useful first extension, without waiting
for an exhaustive anomaly catalogue or proof of universal GA superiority.

## Intended first observation

A workflow receives data, another workflow changes the corresponding persisted value,
and the first workflow later reuses its older data in a write that removes that change.
The report must identify the original observation, intervening committed write, actual
input reused by the overwriter, and resulting committed version. Recovery and normal
writes use the same rule, with the writer phase retained in the finding.

Proposed initial scope: copied values whose origin is traceable through supported DTO
instances and collections. Computed replacements, such as an increment derived from an
old balance, need transformation provenance and remain a separate scope decision. This
bounded observation belongs to the lost-update family; it must not be called complete
P4 detection. A genuinely intended restoration through an old DTO can still match the
observation: programmer intent is not inferable from this evidence.

## Why the existing observations alone are insufficient

The framework records returned outer aggregate revisions and transaction-confirmed writes.
It does not currently connect an outbound command's input values to a particular earlier
response. An aggregate may contain many fields that were not returned in that response.
Two writes, a repeated value, or a decreasing version stamp alone do not establish that
one write used stale input. The fresh-name control even decreases the stored event/version
stamp while preserving the name, demonstrating why the field value matters.

## Proposed smallest implementation direction

The user rejected requiring hand-written DTO/command/persistence mappings. The earlier
manual-projection direction below is superseded by extracting supported copy relationships
from application code. Adding normal application tests to supply useful setup histories
is acceptable; requiring custom prerequisite providers for every experiment is not the
intended user workflow. Reuse the existing source-derived setup pipeline.

1. Record an origin for supported DTOs when the local gateway returns them to a Saga,
   after deserialization. Include supported nested DTOs and the concrete read occurrence.
2. At the next outbound command boundary, before serialization, inspect supported input
   graphs using the extracted contracts. Link reused DTO instances to those origins and check the selected values
   still equal the recorded read values. A new collection containing the same DTOs can
   preserve their origins; an unrelated newly constructed DTO must not be matched merely
   because its values or variable names happen to be equal.
3. Extract direct getter/setter copy relationships from constructors to connect input
   values to persisted attributes. Collection entries need stable identity keys, not list indexes. Verify the
   actual persisted value against the claimed input before deriving a finding. An input
   that the handler ignores must not produce a positive.
4. Join the input/read evidence with intervening foreign writes and the overwriter's
   committed result. Multiple ambiguous command/write matches remain unknown. Prefer a
   first contract with unique action/target attribution over adding a broad tracing system.
5. Keep findings, coverage and originating evidence separate from the numeric search
   preference. Add the supported observation as an explicit selectable criterion while
   preserving the original four-criterion configurations and historical I policy.

The matching/detection logic belongs in the verifier. Supported relationships are inferred
from code, not supplied as Quizzes-specific configuration. Runtime evidence must connect
the returned input object, actual constructor invocation, placement in a registered
aggregate, and matching committed version. The first experiment uses the simulator's
`aggregateId` identity convention and infers destination key names from assignments.
Unrelated dummyapp shapes exercise different type and field names. The new origin scope must not silently widen
the existing compensated-read diagnostic or change its coverage/results.

## Concrete paths to investigate automatically

- GetTopicById -> TopicDto: identity, returned revision and name.
- GetTournamentById -> TournamentDto: original settings and nested Topic DTOs, with each
  Topic's origin represented within that particular Tournament response.
- UpdateTournamentCommand: preserved tournamentDto/topicDtos references, target Tournament
  identity, scalar settings and keyed embedded Topic values actually replaced by the handler.

The existing command constructor stores the supplied DTO references without cloning.
The forward workflow stores Topic responses and its getTopicsDtos method only constructs
a new Set around those same objects. Compensation passes the saved Tournament DTO and
its nested Topics. These properties make object-origin tracking plausible for both proven
cases. They do not demonstrate that every future application will preserve object identity.

## Acceptance direction

- Reproduce both real positive histories through the production observer and assessor,
  retaining the three controls and both local serialization modes.
- Dummyapp coverage for different type/field names, scalars and keyed collections,
  reordered collections, fresh reads, unrelated-field writes, ignored command values,
  untracked/cloned/changed inputs, missing origin and ambiguous writer joins.
- Preserve baseline exclusion, observational non-interference and existing I/A results.
- Verify that the new explicit weight affects fitness without changing existing policies.
- Document unsupported transformations and paths as coverage boundaries, not scored zero.

## Boundary established by the initial proof

The initial experiment established automatic inference plus runtime instrumentation for
copied values. Its mutable object indexes were experimental, and its five histories were
controlled schedules. The subsequent integration below replaces those indexes with
attempt-scoped observation and adds ordinary generated-case qualification. Computed stale
values remain outside the supported slice.

## Integration outcome

The ordinary observer, generic assessor and fifth selectable criterion implement the
approved copied-value slice. The ten integrated controls retain four positives and six
negatives. See [current behavior](../current-state.md#stale-write-research-evidence) and
[retained integration evidence](../evidence/lost-copied-update-2026-09-15/README.md) for
generated-case qualification, coverage, compatibility and reproduction.
