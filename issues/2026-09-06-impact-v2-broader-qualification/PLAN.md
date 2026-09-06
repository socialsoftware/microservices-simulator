# Execute ImpactV2 broader qualification

Status: complete on 2026-09-06; see `HANDOFF.md`.

`BRIEF.md` owns the full campaign. These steps cover its historical-family slice.

1. Validate the freshly generated current package, identify the benchmark from Saga and
   schedule structure, derive its canonical vectors, and use the existing on-demand
   service for missing vectors.
2. Freeze the post-request manifest, linked hashes, workload ID, current FaultScenario
   IDs, normalized action keys, and the retained historical 34-row comparison metadata.
3. Prepare one shared simulator/verifier/Quizzes build. Run current benchmark rows with
   at most two disposable `scenario-executor` Compose containers in parallel; every row
   receives a fresh JVM and in-memory H2 database. Do not build or pull inside the run.
4. Strictly join package selection, execution, ImpactV1, and automatic ImpactV2. Join
   the application benchmark observation when available; otherwise preserve its
   pre-execution rejection without treating it as an execution or ImpactV2 result.
5. Publish status-separated results and the exact schedule-space difference. Update the
   current-state and roadmap only after runtime evidence exists, then record validation
   and handoff evidence.

The historical labels are comparison metadata. The selector and runner do not read them
when choosing scenarios or invoking the assessor.
