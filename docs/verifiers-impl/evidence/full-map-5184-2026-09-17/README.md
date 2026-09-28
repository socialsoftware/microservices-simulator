# Full 5,184-case measured map

The campaign was launched on September 17 with three isolated Docker executors, following
the local 2/3/4-worker capacity pilot. It measures every candidate once using the current
qualified runtime, including the exclusive-field residual assessor. It does not run the
historical GA/per-vector-random campaign again. Recorded-feedback comparison of final GA
with uniform random and the preselected repeatability checks are separate subsequent steps.

## Scope and qualification

- Workload: `ef019eb258c89262dddfeed7279c00a43b2620e2854274aefdefb4f644becf2a`.
- Fixed forward workload: AddParticipant → UpdateTournament → LeaveTournament → RemoveTournament.
- Existing complete structural enumeration: 216 canonical vectors, 5,184 distinct candidates;
  no truncation. A private copy of the catalogue and original input package is retained.
- Fresh no-fault control: COMPLETE, SUCCESS, EXACT, all five unit-weight criteria complete zero.
- Runtime: the source-qualified runtime used by the catalogue-live/capacity pilots. No new
  application behavior, detector criteria, weights or GA operators are introduced.
- Unavailable assessments remain unavailable and consume one map entry; they are not retried
  until a preferred result appears. The completed map will not imply that all scores exist.
- Storage projection from the historical four-Saga attempts: 6.89 GiB for files inside
  attempt directories, excluding shared package blocks, outer logs and campaign summaries.
  Launch free space was 15.22 GiB; the supervisor requests a draining pause below 2 GiB.
- Docker build cache cleanup reported 5.649 GB reclaimed. Images, containers and volumes were
  not pruned; the frozen application image successfully executed the fresh control afterward.

Raw evidence and current status: `verifiers/target/full-map-5184-2026-09-17/`.
`map/status.json` is the live authority; this note is not a completion claim.
`qualification.json`, `evaluation-intent.json`, `storage-estimate.json`, `launches.jsonl`
and `map/protocol.json` record the qualified scope and execution configuration.

## Operate from the repository root

```bash
# Progress: measured / total, inFlight, stage.
python3 verifiers/target/full-map-5184-2026-09-17/manage.py status

# Graceful pause: stop dispatching and let current attempts finish.
python3 verifiers/target/full-map-5184-2026-09-17/manage.py pause

# Resume after a pause or unexpected interruption.
python3 verifiers/target/full-map-5184-2026-09-17/manage.py resume
```

Before closing the lid, wait for `PAUSED` (or `COMPLETE`) with `inFlight: 0`. The detached
launcher keeps running independently of the Codex turn/terminal and uses `caffeinate -i`
to prevent idle sleep while active. Keep the laptop powered with the lid open for an
uninterrupted run; the display may be locked or turned off. This does not make lid closure
safe during an active attempt.

After a reboot or unexpected stop, start Docker Desktop, ensure the prior campaign process
and any orphaned attempt containers have stopped, then use `resume`. The runner refuses
a second coordinator and overlapping orphan containers. It validates frozen inputs and
finished report hashes, preserves finished unknown scores, and retries only attempts that
never produced a complete attempt record. A forced interruption can lose an in-flight
attempt. A process left alive across laptop sleep may also hit a wall-clock timeout; use
graceful pause before planned sleep. A stale status file after a crash is not proof the
process remains alive. Inspect `campaign.log` if resume refuses.

Resume does not rebuild the runtime or accept changed configuration/runner hashes. Preserve
those inputs during the campaign. The `manage.py` launcher belongs to this retained campaign;
`measure_map.py` remains the reusable measurement runner.

## Intended figures

The main curve plots distinct candidates selected (x) against known positive scenarios
found so far (y). Final GA and uniform random receive a recorded result only after selecting
that candidate. At complete coverage they have seen the same outcomes; their discovery order
can differ. A second curve tracks accumulated available weighted score, since finding many
low-score positives and finding fewer high-score positives are different objectives. Retain
unknown counts alongside both. These are discovery-order comparisons, not live runtime
measurements or counts of distinct bugs.
