# M2 handoff — current executable package

- **State:** current-package implementation and local focused proof complete; no commit made.
- **Boundary:** executable role publication, current-only package reading, setup/preflight/execution projection, and on-demand fault-scenario requests.
- **Preserved state:** unrelated edits in `docs/verifiers-impl/current-state.md` and untracked `lib/` and `tmp/` were not changed.

## Implemented behavior

`ExecutableArtifactWriter` extends the static package with reusable `setups`, plan-local `workloads`, compact `faultScenarios`, and one package-level `requests` stream. Source setup bindings use canonical input-fact identity, so a single binding can serve every participant consuming the same input. The reader validates that every consuming workload can resolve those bindings. Schedules and compensation checkpoints retain exact Saga-local step ids, including occurrence suffixes; repeated same-name steps remain distinct.

`ScenarioCatalogPackageReader` is current-only at ordinary read and selected-execution boundaries. It validates exact manifest roles, safe package paths, hashes, UTF-8/JSONL, setup/input/Saga/interaction references, exact schedule-to-Saga step identity, event routes, compact fault actions, and compensation references to an exact earlier completed forward occurrence. Projection preserves source setup recipes, declared binding types, DateHandler transforms, and event metadata, and deduplicates shared accepted input facts. Execution, preflight, and impact outputs remain outside the package while their report references and package paths stay valid.

The adapter boundary now distinguishes all extracted input facts from the pre-existing generation-eligible input list. Static artifacts persist all facts, while `ScenarioGenerator` and prerequisite generation receive only the same accepted inputs they received before M1, retaining their raw source identities for canonical source-setup binding. No prerequisite/materializability selection algorithm was broadened to make the Docker proof pass.

On-demand requests use the accounting cap by default and record any override in request identity. A smaller-cap request followed by a larger-cap request adds only absent schedules; exact repeats deduplicate. Faults, requests, accounting current totals, and manifest hashes publish atomically while initial totals remain unchanged.

## Test migration and coverage

The executor suite now constructs and consumes production current packages. The old 145-test run had 42 failures and 4 errors caused by helper-era aliases, embedded manifest materializability, internal compact-field mutations, and v3 dynamic-publication compatibility cases. Those historical-only cases were removed, while current recovery, event, preflight, source/provider setup, materialization/startup gates, output isolation, checksum, selection, and CLI behavior were retained and migrated. The resulting `ScenarioExecutorSpec` has 123 passing tests.

The 830-line legacy `ScenarioCatalogJsonlWriterSpec` was removed in favor of the current role/hash/reference negative matrix and direct current on-demand operational tests. `DummyappEventConsequencePackageSpec` now drives the production executable writer. A production-writer exact-shape test uses the single M0 fixture/constructed internal models rather than helper-written package files and proves setup source/provider, workload step/event schedule, fault actions, and requests. Repeated same-name Saga steps are covered through production write/read with `same#0` and `same#1`, including compensation of the exact completed occurrence.

The 23-case on-demand replacement covers smaller-then-larger caps, default and override identity, deduplication, only-absent additions, initial/current accounting, hash refresh, and deterministic request/fault ordering across equivalent packages. Operational coverage proves process-local serialization across aliased manifest paths; canonical OS package-lock acquisition and release; lock-resource, acquisition, and release failures; atomic-move fallback; and the defined cleanup-failure behavior after a valid revision is already published. A parameterized matrix injects failure at all eight current staging/promotion boundaries and proves byte-for-byte rollback/preservation of the manifest, accounting, fault, and request artifacts, current-reader validity, and staging cleanup. These tests contain no historical package-schema assertions.

Coverage retained from the deleted suites includes manifest role/path/hash validation, package reference integrity, setup/provider/source projection, materialization and startup gates, schedule and event execution, recovery and exact compensation, selection/CLI behavior, output isolation, and on-demand publication atomicity. Historical schema aliases, embedded materializability, legacy dynamic-publication mutation, and compatibility-only pseudo-version assertions were intentionally not retained. `ScenarioCatalogJsonlWriter` itself remains isolated because M1 dynamic-enrichment/accounting fixture tests still use it; it is not an ordinary current-package reader/writer path. Two current-contract tests also reuse its deterministic SHA helper. Removing that M1 support would exceed this milestone's preservation boundary.

## Local proof

The combined focused command ran generator, prerequisite generator, static/current writer, dummyapp event/accounting/dynamic, on-demand, executor, recovery, model, and source-derived integration suites: **335 tests, 0 failures, 0 errors, 0 skipped** after adding the 18 operational on-demand cases.

A subsequent ordinary `cd verifiers && mvn clean test` rebuilt all production and test classes from a genuinely clean `target/` and ran **646 tests, 0 failures, 0 errors, 0 skipped**. This proves the deleted legacy test class was not present or runnable. `mvn -q test-compile` and `git diff --check` then passed.

## Docker proof

The first generator attempt reached the compose memory limit. The successful runs set host compose limits (`MEDIUM_MEM_LIMIT=5g`, `MEDIUM_MEM_RESERVATION=2g`) and kept the JVM heap below that limit (`-Xmx4g`); container logs showed complete runs rather than OOM-truncated output.

Ordinary bounded runs were used only to diagnose deterministic workload priority. The latest such valid package is `verifiers/target/quizzes-20260902-000100-169` (1,000 workloads); it did not contain a source-derived setup and is not the replay proof package. Once that fact was established, no further 1,000-workload generation was run.

The authoritative fresh proof package is `verifiers/target/quizzes-source-current`, generated inside the verifier Docker test container through `SourceDerivedSharedSagaWorkloadAnalysisSpec` and the production writer with one deliberately selected source-derived workload. It contains one materializable source setup, one workload, and 13 persisted fault scenarios. The setup has 12 source-derived actions and four canonical bindings; the package has the current manifest and eight current role files, including an initially empty package-level requests stream.

Setup preflight wrote `verifiers/target/scenario-executor/m2-preflight.json` and exited successfully: one candidate, two participants, workload and both participants `SETUP_READY`, all 12 setup actions succeeded, all four bindings resolved, and two pending events were cleared to an empty baseline.

Persisted replay selected fault scenario `16844644a5ba8d4b6c2d2ae7823e0dfc9e8ac8d5b271cb3ee099add097d26ac4`, wrote `verifiers/target/scenario-executor/m2-replay.json`, and exited successfully. Source setup succeeded, schedule conformance was `EXACT`, and the terminal status was `PARTIAL_COMPENSATED`, including the assigned fault and exact persisted compensation action sequence.

Before the clean build, the authoritative proof package and both reports were copied to a private temporary backup. After the successful clean suite they were restored byte-for-byte to the same cited `verifiers/target/` paths; all cited Docker artifacts therefore exist in the final workspace.
