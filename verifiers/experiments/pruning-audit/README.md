# Pruning audit

This experiment checks which pairs the production interaction rule discards, independently
of segment compression. It does not modify Quizzes, production generation or scoring.

## Selection

Extract the current Quizzes sources/tests with the existing visitors and normalize all
eligible inputs (cap 1,000, checked to exclude none). For every pair of distinct Saga types,
compare `ALL` with `WITH_TYPE_ONLY_FALLBACK` using production `InputTupleSelection` and
`ConflictGraphBuilder`. Save input identities, direct accesses, compensation accesses and
one-hop event routes whose downstream Saga may conflict with the other Saga by aggregate
type. These latter annotations are investigation candidates, not exact object bindings.

Four probes were chosen by structure before application outcomes:

- UpdateStudentName + FindTournament: event-mediated relation.
- GetCourseExecutionById + FindTournament: queries to different aggregate types.
- UpdateUserName (Tournament service) + DeactivateUser: compensation-only access to User.
- UpdateTournament + FindTournament: retained direct update/read relation.

For each probe, choose the lexicographically first input-ID pair from the same source
class/method. If no such pair exists, record that gap instead of joining arbitrary fixtures.
This rule deliberately does not promise successful preparation, a relevant event recipient,
or a healthy control. Qualification must establish those facts; revise and record the
selection rule if the original source preparation cannot exercise the intended relation.

Generate ordinary packages with pruning on/off, full order-preserving interleavings and
up to three event consequences. The generator receives every extracted Saga definition
but only the selected pair's inputs; this preserves focused enumeration while allowing
event-mediated selection to inspect its non-participant downstream Saga. Events are
included identically on both sides. No segment compression. Enumerate no fault or one
forward-step fault per Saga, allowing faults in both Sagas, and every recovery order.
Assert no cap truncation. These are catalogues, not runs.

## Run

```sh
python3 verifiers/experiments/pruning-audit/prepare.py \
  --build verifiers/target/rq1-static-pilot-2026-09-18 \
  --output verifiers/target/pruning-audit-NEW
```

The preparation verifies the isolated build's source hashes, freezes application and
analysis sources, compiles only the harness, extracts once and generates the probes.
`EnumerateFaults.java` takes generated package directories, uses the existing
`OnDemandFaultScenarioService`, and prints JSON enumeration receipts. Compile it with the
same saved classpath, then run against each `*/BRUTE_FORCE` directory, saving stdout as
`fault-enumeration.json`. Use Java 21. Freeze its source alongside `PruningAudit.java`.

```sh
python3 verifiers/experiments/pruning-audit/analyze.py verifiers/target/pruning-audit-NEW
```

The analysis verifies every package manifest hash, retained-subset identities, unique
scenario IDs and untruncated request receipts. Preserve generated packages and raw inventory.

## Runtime stage, still pending

First qualify preparation and no-fault controls, including actual event eligibility. A
successful empty delivery is not evidence that the intended event interaction was exercised.
Event-producer attribution is already a known limitation of the current read detector.
Keep incomplete criteria explicit; do not substitute zero or require them to look complete.

Before a preservation claim, compare pair observations with each Saga executed alone under
matched faults and an equivalent initial domain state. Removing the other Saga must not
silently alter preparation, object identity or relevant event routes. Those isolated controls
are not yet generated here. Compare finding signatures and report co-occurrence separately.
A positive in a discarded pair can reproduce a defect already visible in one Saga alone.

The post-fix static regeneration is recorded in
`verifiers/target/pruning-audit-2026-09-19-review/`: 16/16 event-only plans are retained,
the 0/2 unrelated-query control remains pruned, and the 6/6 direct pair is unchanged.
Its 170 canonical scenarios are enumerated without truncation, but none was executed.

Later receiver qualification, the event-writer attribution correction and a fresh input
selection recount are documented in
`docs/verifiers-impl/evidence/event-read-attribution-2026-09-19/RESULTS.md`.
Run `PruningAudit <applications-root> quizzes <new-output> selection-counts` to count
all input combinations of two through four distinct Saga types using the direct and
augmented production graphs. It writes per-set rows and totals, without enumerating
schedules, event placements or faults. Use the same source/build verification as above.

## Bounded preservation follow-up

`PreservationProbe.java` shares extraction with the generation-comparison harness and
writes ordinary packages with singles included, full order-preserving interleavings and
identical event settings on both sides. The three structural probes are two independent
queries, an Execution query with a Tournament update, and the retained Tournament
update/query relation. They are not selected by measured score.

For each pair, take the first lexicographic input-ID tuple from the same source class and
method. `EnumerateFaults` expands every canonical per-Saga forward fault and recovery
sequence without truncation. The native runner checks that pair and isolated workloads
have exactly the same source setup actions and matching argument bindings; no hand-written
fixture or runtime ID substitution is performed. Every normal control must be SUCCESS,
EXACT and complete-zero before its faulted variants run. Keep rejected controls. An
optional third `PreservationProbe` argument selects a subsequent tuple index for the
Execution-query/Tournament-update probe; record any qualification-driven progression.
The September 20 run rejected index 0 and admitted index 1, without trying further tuples.

`run_preservation.py <generated-root>` verifies the existing frozen Quizzes runtime and
qualified event-read overlay, uses two fresh native JVMs at most, preserves receipts and
refuses implicit retries or less than 2 GiB free. This is local functional evidence,
not a Docker or performance benchmark. Its pinned runtime paths are intentionally local.
`analyze_preservation.py <generated-root>` independently validates package/report hashes,
compares initial domain state, normalizes finding identities using the prior compression
study's projection, and matches each discarded pair against single-Saga executions with
the same inputs and faults. It checks both finding unions and the final domain state
composed from the isolated changes. Conflicting isolated changes cannot be composed.
Unknown or deviating observations prevent a complete preservation result.

Reusing a finding signature does not equate pruning and compression: pruning removes
whole combinations, while compression chooses orders within a combination. A finding
preserved in an isolated scenario may occur in fewer positive executions afterwards.
This small comparison is evidence for its exact inputs and detector scope, not a universal
claim about arbitrary application semantics or all generated combinations.
