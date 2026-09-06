# Execution plan

## Environment and execution mode

Documented route; direct implementation in `fault-analysis/scenarios`, no worktree or
subagents. The user explicitly authorized proceeding after documentation without another
checkpoint. Preserve `note-04-09-2026.md` and the meeting note. Local linear commit only.
Coordination amendment: another authorized task may change Java/Groovy extraction in
this checkout. Validate the measured snapshot itself and record checkout drift separately;
do not invalidate a snapshot campaign because unrelated current sources changed. Stage
only this task's files/hunks. Restrict canonical edits to batch/search sections and
coordinate heavy Docker validation with task `01a078c8-2203-76f3-8bfa-4047a6ef9fbd`.
Reuse the source-matched combined-event run-02 Docker build; do not compile in mutable
checkout targets. Canonical current-state/roadmap updates accompany the harness.

## Implementation strategy

Extend the existing Python attempt primitive with explicit assessment mode while keeping
regression behavior as default. Add a narrow workload-level freeze/strategy/evidence
driver; ordering reads only scenario IDs/vectors, never reference or observed scores.
Use the current combined-event package (same frozen current sources), copied into a new
directory. Request only missing benchmark vectors through FaultScenarioRequestCli;
never mutate retained packages or request eager vectors again.

Preselected workloads, before new outcomes:

- Corrected RemoveTournament/AddParticipant: exact existing workload
  `435ac86830d1ad7d778abe6dd4e046f728c9f3fd695106657aa430bc2d1710fc`; no event delivery,
  two participants, 12 canonical vectors, expected structural universe of 29 schedules.
- CreateQuiz: qualified basic workload
  `a8aa42c2fc0d5b41eb5e644a6fda8fbcba2ff3b199223cc549106b5ef896e5c7`;
  single participant, three forward steps, no event. Select all four persisted zero/
  single-fault scenarios, not only the earlier control/last-fault pair.
- AnonymizeStudent with its selected event:
  `c2559b797958f4f0c185007cedcc1da6a2eb1b76ef558a593d12d699dec9f60b`;
  single participant, two forward steps and one fixed event consequence. All three
  persisted zero/single-fault scenarios. Its no-event sibling is outside this universe.

Selection reason: concurrent-participant recovery, creation, and event-bearing update
provide different execution structures using previously qualified preparation. No prior
scores for these additional workloads are read for selection or ordering.

Per strategy, budget is 12/4/3 respectively. Twelve equals the benchmark's canonical-vector
count but samples scenarios, not one scenario per vector: recovery variants compete for
the same budget. It intentionally truncates 29 eligible schedules to a prefix of 12.
The small universes are exhausted. One deterministic run and five random seeds
`11, 29, 47, 71, 101` produce 114 planned online attempts. No deterministic replicas.
All arm orders and prefixes are frozen before execution; at most two arms run concurrently.

## Milestones

### M0 — Current package and frozen experiment (FR-2, FR-6, FR-7)

Verify prepared build/source identity; copy current package; request six absent canonical
benchmark vectors only. Assert exact known 29 IDs/actions and 12 vectors against structural
reference selection, without loading score labels. Record generation/request caps and
every eligible candidate. Confirm additional universe sizes 4/3; fail visibly if different.

### M1 — Assessment and strategy contracts (FR-1…FR-5)

Implement explicit unknown-outcome assessment, deterministic/seeded orders, duplicate and
budget checks, and retained attempt/discovery artifacts. Test valid zero/positive,
partial/invalid/incomplete, bad joins, timeout, no-positive, duplicate prevention,
reproducibility, and ordering independence from outcomes. Keep regression tests passing.

### M2 — Online comparison and repeatability (FR-3…FR-8)

Launch all 114 planned attempts without score-based filtering/retries. The deterministic
benchmark is evaluated online; known historical landscape comparison is post-run validation
only. Additional candidates are repeated six times across arms and checked for semantic
stability afterward. Record any invalid/unseen candidate and incomplete coverage honestly.

### M3 — Evidence and integration

Recheck all hashes/joins/unique attempt IDs and frozen plan prefixes. Produce a readable
per-workload discovery table/curve, costs, assessment counts and concrete GA gaps. Retain
raw reports in target and compact evidence in Git. Update canonical docs, write HANDOFF,
review the diff, run relevant tests and make the authorized local commit.

## Validation strategy

Python unit/integration tests for orchestration; current Docker execution as runtime proof.
No Java behavior changes: no stale Maven reports counted. Full semantic report validation,
candidate-plan equality, distinct attempt identities and package/build/dependency hashes
before/after. Offline reference checks are labeled and never counted as fresh attempts.

## Risks and fallbacks

Wrong schema/ID/build: reject and regenerate from current frozen sources if needed, never
translate historical IDs. Infrastructure/timeout: retain and consume one budget slot,
continue without retry. Flat scores or repeated zero: valid finding, never substitute a
different workload. Recovery caps and canonical fault vectors bound the universe and must
be reported; no claim of exhaustive binary schedules. Concurrent arm wall times depend on
host contention, so compare discovery by attempt budget and report timing descriptively.
