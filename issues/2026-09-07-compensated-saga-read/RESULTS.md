# Quizzes runtime qualification results

The fixed M2 matrix completed **22 fresh JVM/Spring/H2 Docker runs**. All commands and
controlled semantic checks passed. The final auditor passed **497 checks**, including
15 full report comparisons across the excluded initial pair, three measured on/off pairs
and the ordinary executor pair. Application values, revisions, lifecycles, dependencies,
execution outcomes and ImpactV1/ImpactV2 results agree across every paired comparison.

## Observed cases

| Case | Runs | Diagnostic result | Runtime proof |
| --- | ---: | --- | --- |
| Split CreateTournament / StartQuiz | 3: JSON twice, direct once | 1 exposure each | A creates SagaQuiz 9/v18; B receives v18; explicit compensation persists v19 DELETED with predecessor v18; B creates ACTIVE SagaQuizAnswer 10/v20 referencing Quiz 9/v18. |
| Compensation before B reads | 3: JSON twice, direct once | 0; failed call, no delivery | The actual AGGREGATE_NOT_FOUND identifies Quiz 9; B creates no QuizAnswer. |
| Successful producer with overlapping read | 3: JSON twice, direct once | 0; NO_SUBSEQUENT_COMPENSATING_DELETION | A creates Quiz, B receives it before A completes, then A and B commit successfully. |
| Read-only FindQuiz | 1 direct plus 8 JSON cost/control runs | 1 in every enabled run; no sidecar when disabled | B receives Quiz 9/v18 and commits before A compensates, with zero B committed writes. |
| Outer Tournament lookup | 2: JSON/direct | 0; baseline revision | Delivered Tournament 10/v19 matches persistence. Its stored nested Quiz reference is 9/v18, but returned nested Quiz.version is null and is not borrowed. |
| Ordinary ScenarioExecutor FindQuiz | 2: on/off | 0 enabled; absent sidecar disabled | Both SUCCESS/EXACT. Enabled actual Quiz 11/v22 delivery matches baseline and is assessed REVISION_PREEXISTS_MEASUREMENT. |

Every enabled run reports `COMPLETE_WITHIN_SCOPE`, zero diagnostic gaps. This applies
only to the two declared outer-response contracts. It does not cover arbitrary reads,
nested references, lists, event consumers or restored updates. Positive input binding is
performed by the controlled harness using the actually created ID. The ordinary control
uses a setup result; no generated runtime result binding was added.

## Fixed cost sample

Fresh-process action costs, milliseconds; one initial pair excluded, with no per-JVM
steady-state JIT warmup. Order was fixed before measurements: on/off, off/on, on/off.

| Pair | Enabled | Disabled | Enabled minus disabled | Separate diagnostic assessment |
| --- | ---: | ---: | ---: | ---: |
| 1 | 161.507 | 237.365 | -75.858 | 13.489 |
| 2 | 170.998 | 189.940 | -18.942 | 7.943 |
| 3 | 177.383 | 162.993 | +14.390 | 47.180 |

Median paired action difference: **-18.942 ms**. These three noisy fresh-process pairs do
not establish a speed improvement, statistical significance, throughput or scaling.
Action timing excludes startup, setup, probes, final snapshots, assessment, JOL and JSON.
Final-horizon durations are retained separately: enabled 25.403/7.981/8.974 ms and disabled
7.875/10.788/29.414 ms. Full process durations include measurement-tool work and must not
be interpreted as production diagnostic overhead.

Each measured enabled reader-only run retained one delivered call, seven unmapped calls,
four excluded calls, two copied writes, eight baseline revisions, eleven actions and one
finding; zero failed/invalid calls or gaps. Sidecar size: **34,964 bytes**. JOL 0.17 measured
**576 objects / 21,560 bytes** reachable from the real diagnostic collector and report
before diagnostic serialization. This inclusive graph includes shared references; it is
not exclusive retained heap or peak allocation. Disabled runs allocate neither diagnostic
collector nor report. Both JVM modes use the same JOL Java agent and clock fixture.

There is no retention cap or silent truncation. Identity/revision joins are indexed, but
per-call histories and fallback action scans can cost `O(R * W_same_identity + R * A)`.
The original globally linear-cost hypothesis is therefore not established. Read collection
adds no persistence query; existing write/snapshot collection remains enabled in both modes.

## Audit corrections and preserved evidence

The first summary reported two failures; it remains `summary-pass1.json`. Neither required
changing or rerunning Java:

1. Its broad directory inventory counted 22 new Logback files as source drift. Every one
   of the 983 frozen source files, 1780 runtime artifacts and nine copied package files
   retained its hash. The corrected audit verifies every original source and permits only
   additions under `applications/quizzes/logs/app-test-*.log`, recording every added hash.
   Original `completion.json` retains `sourceUnchanged: false`; `summary.json.sourceAudit`
   documents the resolution. The future runner excludes that exact runtime-log namespace.
2. Ordinary execution comparison retained setup duration and attempt-prefixed internal
   result IDs. The audit now records and normalizes only `sourceSetup.durationNanos` and
   the exact current-attempt prefix of `sourceSetup` retainedResultId values, preserving
   every setup-action suffix and relationship. Other normalization is limited to
   executionAttemptId, that same identifier in framework injected-fault messages, and
   frameworkMetadata.creationTimestamp. Application data is never normalized.

The executing analyzer hash is recorded independently of the frozen build. The measured
orchestration script matched its frozen copy when the matrix started. The Java sources
and binaries were unchanged throughout. An independent reviewer rehashed the evidence and
accepted these audit-only corrections. No failed timing or semantic sample was discarded.

An earlier host adapter test compilation failed because its test imported QuizType as a
nested enum; the import was corrected to the actual top-level enum. The corrected suite
passed seven tests locally and seven again in Docker. Both host logs and final XML remain
in the evidence archive. M0's 28 and M1's 247 passing tests are documented in their handoffs.

## Provenance and reproduction

- Image: `sha256:0aab59d58bfe4f83e6bee2a4002a913dcbc26d861acee5f0327c053f12918282`, Linux arm64, Temurin 21.0.3+9.
- Per-container limits: two CPUs, 3 GiB; JVM `-Xmx1536m -XX:MaxMetaspaceSize=512m`.
- Build source manifest SHA-256: `34bd20fa18c201d55e13bff68a3cdef5ae8a7c564d9e34dd1fca2832f28408bd`.
- Frozen ordinary manifest SHA-256: `8b055070f81ff1cf504244ab59e64ffa20e4f129e565b3377cbd7e1eec02e873`.
- Durable local archive: `verifiers/target/saga-read-exposure/m2/`, 227 retained files plus archive manifest; archive manifest SHA-256 `1ce6bda36d3a92b72d8970af13d61961712b261cb1f22ed431afdc50ce099268`.
- Original private source/build/cache/reports: `/tmp/saga-read-exposure-28go53x8/`. Artifact references retain their original absolute paths; copied files are byte-identical.

The local archive includes all runs/logs, the initial failing and corrected summaries,
source/runtime manifests, compiler/test proof, 22 application logs, and compressed frozen
source and ordinary package. It is intentionally outside Git under the ignored target
namespace; preserve it before cleaning build outputs. The tracked
[runner instructions](../../verifiers/experiments/saga-read-exposure/README.md) describe
reproduction from the frozen package. No shared build target/cache was used, and no
production service, DTO, lock or transaction behavior was changed for qualification.
