# ImpactV2 broader qualification

Status: qualified on 2026-09-06; see `HANDOFF.md`.

`BRIEF.md` owns the complete 60-case broader sample and 17-case historical-family
campaign context. This spec records the historical refresh slice.

## Outcome

Qualify ImpactV2 on a broader current Quizzes sample and refresh the historical
RemoveTournament/AddParticipant landscape through current package and executor contracts.
The historical slice compares the current generated structural schedule keys with the
retained 34-row benchmark, while keeping the old `HARMFUL_FOR_RULE` and
`NO_BROKEN_REFERENCE` labels outside ImpactV2 assessment.

## Requirements

- Use one current generated package and freeze its linked artifact hashes after all
  authorized on-demand requests.
- Select the benchmark by its two Saga types, exact five-step order, linked setup, and
  absence of event actions. Never select by historical package IDs.
- Derive the 12 canonical vectors from participant ownership. Persist missing vectors
  through `FaultScenarioRequestCli`; do not synthesize FaultScenario records.
- Execute every current persisted schedule for those vectors in a separate JVM and fresh
  H2 database through the ordinary Saga/local `ScenarioExecutor` path.
- Record ImpactV1 and ImpactV2, plus the existing application benchmark observation
  when its setup contract accepts the workload. Preserve pre-execution wrapper
  rejections separately and use the ordinary executor for the provider-backed family.
  Report ImpactV2 `COMPLETE`, `PARTIAL`, `INVALID`, and `UNAVAILABLE`
  separately. A partial observed count is a lower bound; missing evidence is unknown.
- Compare historical labels only after assessment. Labels must never select cases,
  change actions, or enter ImpactV1/ImpactV2 scoring.
- Report exact changes in current schedule count and structural keys. Do not manufacture
  34 current rows when recovery modeling has changed.
- Keep ImpactV2 semantics and production Quizzes sources unchanged.

## Boundaries

This issue may add qualification scripts, checked comparison metadata, ignored runtime
artifacts, and qualification documentation. It may repair a test fixture only when the
current supported contract requires it. It does not change scoring, application behavior,
package schemas, recovery generation, deployment, or search.

## Acceptance proof

The checked selector validates the current manifest and linked hashes, resolves one exact
benchmark workload, derives 12 vectors, freezes current scenario IDs and normalized
structural keys, and accounts for historical matches and absences. Runtime completion
requires immutable source copies and package proof, one fresh process/H2 per current row,
strict execution/V1/V2 report joins, status-separated ImpactV2 results, optional
application observations or explicit pre-execution rejection evidence, and an explicit
current-versus-historical comparison. Missing reports fail campaign completeness and do
not create a synthetic ImpactV2 status.
