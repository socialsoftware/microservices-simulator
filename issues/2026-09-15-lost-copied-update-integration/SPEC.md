# Lost copied updates in ordinary scenario execution

Status: approved for implementation on 15 September 2026, including the counting unit.

## What this feature is

Add the demonstrated copied-value overwrite observation to ordinary Saga/local scenario
execution and configurable search fitness. A Saga receives an old value; another writer
commits a change to the corresponding persisted cell; the first Saga subsequently copies
its old input into a committed write that removes that change. Normal execution and
compensation use the same rule. This reports an observed interaction, not inferred intent.

The feasibility proof is recorded in
[`inferred-stale-write`](../../docs/verifiers-impl/evidence/inferred-stale-write-2026-09-15/README.md).
Its ten controlled executions do not yet establish generated-workload coverage.

## Goals

1. Infer supported constructor copies from source without application-specific mappings.
2. Observe the actual response/input/copy/persistence chain during ordinary execution.
3. Report evidence, coverage and a selectable anomaly count usable by the existing GA.
4. Retain existing measurements and four-weight configurations unchanged.

## Non-goals

Computed stale replacements, arbitrary cloning, setter-based copy inference, remote RPC,
general-purpose data-flow tracking, full P4 detection, new GA operators, RL and a broad
search campaign. No Quizzes business-code changes or per-scenario prerequisite providers.
Normal application tests may supply input histories through existing setup extraction.

## Functional requirements

### Evidence and scope

- FR-1: Infer direct scalar getter/setter constructor copies and stable collection keys
  using the supported source pattern and simulator identity conventions. Retain source
  provenance and deterministic contract ordering. Unsupported inference is explicit.
- FR-2: Start observation after setup and end it with the execution attempt. Release all
  retained object references and restore observer state on success and failure.
- FR-3: Establish the actual returned DTO occurrence, reused outbound input, exact local
  transport pairing, executed constructor, placement in the registered aggregate and
  uniquely matching committed version. Equal values alone never establish origin.
- FR-4: Require an intervening foreign committed change to the same cell after the read,
  followed by the attributed committed overwrite with the old copied value. Preserve the
  proof's immediate predecessor rule. Creating a new cell is not this observation.
- FR-5: Include normal and recovery writes, source read, overwritten and overwriting
  versions, writers, aggregate identity, keyed field paths and values in the finding.
  Identity/version metadata do not themselves count as overwritten business fields.
- FR-6: Missing origins, ambiguous matches, unavailable hooks and unsupported paths have
  explicit coverage outcomes. A positive can remain visible alongside gaps; an incomplete
  enabled criterion cannot yield an available fitness score. Complete means within the
  declared copied-constructor scope, not all possible lost updates.

### Count and search

- FR-7: Counting unit: one overwriting committed version of one aggregate per
  attempt, with all proved overwritten fields and their evidence grouped inside it.
  Repeated hooks and multiple copied fields do not multiply that occurrence. Two distinct
  committed overwrites count twice. This counting unit was approved with this package.
- FR-8: Expose `LOST_COPIED_UPDATE` as an independently weighted criterion. Keep raw
  persistent-object counts and compensated-read counts intact; expose the new count
  separately rather than silently redefining the historical A field.
- FR-9: Existing legacy and four-criterion policies retain their configuration, scoring
  and missing-data behavior. Introduce an explicit five-criterion policy version; zero
  disables a criterion's contribution and coverage requirement. Reject unknown weights.
- FR-10: Ordinary execution emits the evidence and report used by search and rescoring.
  Old reports lacking the new evidence cannot be rescored with a positive new weight.
  Fitness affects GA selection, not scenario generation or random sampling.
- FR-11: Qualify both positive histories and their controls through the integrated path,
  with local serialization off/on. Separately demonstrate at least one positive generated
  workload with source-derived setup; do not describe controlled schedules as generated.

## Architecture

Inference belongs to the existing verifier analysis pipeline. Application source is input,
not rewritten business code. Framework observation is passive; the verifier owns matching
and classification. Transaction-confirmed snapshots remain persistence authority.
The existing read-exposure collector retains its scope and lifecycle. Constructor
instrumentation must be limited to inferred targets and installed before those targets load.
The experimental process-wide mutable indexes are not an acceptable integration boundary.

## Data model

Retain versioned inferred copy contracts, attempt-scoped origin/call/construction records,
registered-copy and committed-write links, grouped overwrite findings, and coverage reasons.
Use complete aggregate identity and attempt identity in joins, not an integer ID alone.
Deterministic IDs refer to execution occurrences; collection paths use inferred keys.

Proposed glossary entry: **Lost copied update (future)** — an evidenced committed overwrite
that reuses a workflow's earlier copied input and removes an intervening foreign change.

## Security model

No new external service or transmission. Source and runtime must match the inferred
contracts. Instrumentation must not execute extra domain operations or alter outcomes.
Hook failure becomes a coverage gap and must not replace the application's own exception.

## Operating

An explicitly enabled observer is wired into the ordinary supported executor launch path.
Record contract/runtime compatibility and instrumentation availability. Disabled or absent
observation is unavailable, not a negative. Keep existing launch paths usable without it.
Retained reports support deterministic offline reassessment. No commits, pushes or merges
are part of this package.

## Future roadmap

After qualification, compare GA and random with fixed weights and budgets. Consider more
data transformations only if actual workloads justify them. RL and paper evaluation prose
remain separate work.

## Open decisions

No open product decisions. The approved unit is one overwriting commit per aggregate,
with fields grouped; per-field counting was considered and rejected for this version.

