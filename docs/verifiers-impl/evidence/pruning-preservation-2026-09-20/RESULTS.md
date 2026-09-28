# Pruning preservation with matched isolated executions

Two discarded Quizzes combinations were executed with every normal step order and every
canonical forward-fault assignment (no fault or one fault per Saga), including all generated
recovery orders. Segment compression was disabled. Each Saga also ran alone with the same
inputs and preparation. The current pruning rule keeps those isolated workloads and rejects
the two pairs.

| Comparison, including normal controls | Before pruning | After pruning | Positive before | Positive after | Distinct finding signatures before / after |
| --- | ---: | ---: | ---: | ---: | ---: |
| GetCourseExecutionById + FindTournament, plus each alone | 12 | 4 | 0 | 0 | 0 / 0 |
| GetCourseExecutionById + UpdateTournament, plus each alone | 106 | 8 | 38 | 2 | 1 / 1 |
| Total | 118 | 12 | 38 | 2 | 1 / 1 |

All 118 measurements have exact execution conformance and complete counts for the five
criteria. There are no unavailable scores in this admitted comparison. A separate eight
normal controls from the first update input were rejected and are retained below.

## What was preserved

GetCourseExecutionById reads the course execution; UpdateTournament changes the Tournament
and its Quiz. The query does not introduce a detected interaction with that update.
A fault late enough in the update triggers compensation which rebuilds the Tournament's
topics without their course identifiers. The final Tournament therefore differs from the
initial one: `topicCourseAggregateId` changes from `1` to `null` for both selected topics.
This is one affected Tournament under FAILED_OPERATION_RESIDUAL, not two affected topics.
The same difference occurs when UpdateTournament runs alone.

The unpruned catalogue includes 98 pair scenarios and eight isolated scenarios. There are
36 positive pair scenarios and two positive isolated scenarios. Removing the pair therefore
reduces the number of positive executions from 38 to two, while preserving the same finding.
Each of the 98 pair executions matches isolated executions with the same named faults in
its participants. Their finding union and combined final domain state match the pair.
The two-query comparison similarly matches all eight pair executions to its four isolated
scenarios, with no findings or changed final domain values.

The state comparison includes object identities, lifecycle, application attributes and
declared dependencies. It excludes framework row/version/lock metadata, as in the earlier
compression comparison. Finding signatures retain the affected object, reason, normalized
Saga/action identity and its before/after domain state. This establishes preservation for
these inputs and observations; it does not prove all business properties or all discarded
Quizzes combinations. Simultaneous occurrence in one execution is not claimed for separate
isolated tests.

## Selection and qualification

The probes were chosen by structure: independent queries, an independent query/update pair,
and a directly interacting Tournament query/update pair. The last pair's complete catalogue
is unchanged by pruning: 106/106 exact scenario records, including isolated cases. It was
not rerun in this follow-up; no runtime result is imported from it.

For each pair, inputs were sorted by deterministic ID and required to come from the same
source class and method. The first query/update tuple failed seven of its eight normal
controls: the application reported “Not enough questions to generate quiz”, including when
UpdateTournament ran alone. Those controls were not healthy executions of the planned order,
so no injected-fault variants were run for that tuple. Their one residual finding and
matching isolated result remain diagnostic evidence, outside the main table.

The next tuple by the same ordering passed all eight normal controls, with zero findings;
all 106 scenarios were then measured. No further tuples were tried. The two-query tuple
passed all four normal controls. The experiment did not patch inputs, fixture actions or
runtime IDs: the ordinary generator supplied all pair and isolated packages. Their source
setup action lists are identical within each comparison, isolated argument bindings are
subsets of the pair's bindings, and all observed initial domain states agree.

## Verification

- All package manifests and per-attempt report hashes were checked. Cross-report attempt,
  workload and scenario identities agree; source packages stayed unchanged during execution.
- The full fault catalogues were enumerated without truncation through the production
  request API. Retained scenario records are exact members of the unpruned catalogue.
- Independent analysis compared every pair outcome with matched isolated outcomes; all
  106 admitted pair executions match (eight query/query plus 98 query/update).
- The 126 physical attempts comprise 118 admitted scenarios and eight rejected normal
  controls. No retries or hidden replacement of failed observations occurred.
- Execution used fresh native Java 21 processes, two workers, the verified frozen Quizzes
  runtime and qualified event-read assessor overlay. Docker did not respond within ten
  seconds. These are functional observations, not Docker timing or parity measurements.
- No Quizzes application, scoring, search algorithm, cluster deployment or paper change was made.

Harnesses: `verifiers/experiments/pruning-audit/PreservationProbe.java`,
`run_preservation.py`, `analyze_preservation.py`, sharing the existing extraction,
on-demand enumeration and observation projection helpers.

Raw packages, commands, logs and receipts:
`verifiers/target/pruning-preservation-2026-09-20/` and
`verifiers/target/pruning-preservation-2026-09-20-next1/`.
Compact comparison details, selected input identities and the full verification receipt are
retained alongside this report. The earlier 98-to-40 result remains a separate experiment
about segment compression within a workload.
