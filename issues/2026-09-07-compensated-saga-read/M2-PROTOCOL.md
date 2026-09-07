# M2 runtime proof and marginal-cost protocol

Fixed before the first new diagnostic-enabled Quizzes Docker run. Implements the approved
PLAN validation strategy; it introduces no numeric performance acceptance budget.

## Isolation and provenance

Use the `scenario-executor:latest` JDK21 image, recording its immutable image ID/digest,
with a private source/build snapshot, reports directory and Maven repository. Do not mount
the campaign's `m2_cache` volume or write shared application/verifier targets. Coordinate
Docker capacity with the campaign; one qualification container at a time. Retain source
and artifact hashes, exact commands, logs, versions and test summaries. Every runtime
case starts a fresh JVM, Spring context and H2 database.
Qualification containers use two CPUs and a 3 GiB memory limit, with
`-Xmx1536m -XX:MaxMetaspaceSize=512m`. A necessary build-only resource adjustment must be
recorded separately and must not change the paired runtime limits. The preflight image ID
is `sha256:0aab59d58bfe4f83e6bee2a4002a913dcbc26d861acee5f0327c053f12918282`
(`linux/arm64`); pin that image for the matrix rather than resolving a changed tag.

## Qualification matrix

- Run split `CreateTournament`/`StartQuiz`, compensation-before-read and successful-A
  controls twice with serialization enabled; exercise each once with serialization off.
- Run the reader-only positive with both serialization modes. Its serialized repetitions
  may be the enabled runs in the cost protocol below.
- Qualify the outer Tournament adapter and retain the nested Quiz reference's unsupported
  revision as an explicit coverage boundary in both serialization modes.
- Exercise ordinary ScenarioExecutor sidecar persistence with the frozen setup-backed
  single `FindQuiz` zero-vector control; run enabled and disabled counterparts. The copied
  package and pre-outcome selection are recorded in the isolated reports directory.

The controlled positive receives the actual aggregate ID created during A. The ordinary
control consumes a setup result. Report these as distinct proof surfaces. Do not add or
imply generated runtime result binding.

## Cost and equivalence

Use the serialized reader-only positive as the fixed representative cost case. Keep
existing ImpactV2/write collection enabled in both modes. Perform one excluded warmup
pair, then three measured pairs, all in fresh JVMs. Order: warmup off/on; measured pairs
on/off, off/on, on/off. Do not change repetitions based on observed duration.

Measure the workflow action interval, excluding JVM/Spring startup and fixture setup;
record startup/total wall time separately when available. Report each sample and the
median paired difference, with no claim of statistically significant speed difference.
Compare application outcomes and persistent facts, action/conformance outcomes, ImpactV1
where produced, and ImpactV2 assessments. Document every normalization of volatile run
metadata; never normalize away application values, returned revisions, lifecycle,
dependency structure, coverage or scores.

Record delivered/unmapped/failed/excluded calls, copied committed-write count, retained
diagnostic metadata/finding counts and serialized sidecar bytes. Measure the graph reachable
from the diagnostic collector and report together at the completed observation horizon,
before JSON serialization and outside the timed action interval. Use experimental-only
JOL 0.17 (`jol-cli-0.17-full.jar`, SHA-256
`ea8cf31b7dc6c18810ca7aeadcbe7a2b352fb250261b8060ce513e8a99ebcd12`) as a Java agent,
recording JVM options, graph object count and total bytes. Include the actual collector
root, list/map containers, strings and arrays. Label this as an inclusive reachable graph
footprint, a conservative bound including references shared with ImpactV2/source evidence;
it is neither exclusive retained heap nor peak allocation. Do not add JOL to production
dependencies. The [JOL implementation](https://github.com/openjdk/jol/tree/0.17) owns the
measurement semantics. Its record/list/map smoke passed locally under JDK21 before these
runtime experiments; repeat the tool check inside the qualification container.

Optional post-GC used JVM heap is total live heap including application/framework state;
the enabled/disabled difference is only a coarse control. Inspect collector/assessor
complexity and report retention limits; this fixed case does not empirically establish
scaling, and no silent truncation is allowed.

Runs that fail a semantic check remain failed evidence. Fix an in-scope root cause, then
record the superseding source/build and rerun the affected matrix; do not retain only
favorable timing or correctness samples.
