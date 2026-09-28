# Quizzes workload and fault-scenario counts

All accepted inputs: **819**, across **37 Saga types**. Sets of two, three and four
distinct Saga types: 666 + 7,770 + 66,045 = **74,481** sets per configuration.
All totals completed; no inputs removed by a per-Saga cap. No application execution
or cluster compute was used to obtain these totals.

Unlike the older normal-order table, this comparison includes the supported event
selections and their positions, canonical fault choices and recovery orders. The event
limit is three deliveries from one emission site, covering the largest extracted
Quizzes route group. Events from different emission sites are not combined in one
workload: that is the existing generator's rule, not a counter approximation. Consumer
Sagas already present as explicit participants are excluded from event expansion.

| Sagas per workload | Reduction | Workloads | Fault scenarios |
| ---: | --- | ---: | ---: |
| 2 | None | 21,004,894 | 2,705,719,309 |
| 2 | Pruning | 18,029,104 | 2,605,869,896 |
| 2 | Pruning + compression | 5,576,751 | 554,608,026 |
| 3 | None | 5,197,269,339,786 | 128,105,176,663,158,760 |
| 3 | Pruning | 5,135,260,779,236 | 128,047,132,345,883,708 |
| 3 | Pruning + compression | 1,196,374,861,913 | 26,262,486,293,964,824 |
| 4 | None | 4,635,657,405,981,397,848 | 57,329,019,050,814,087,908,750,514 |
| 4 | Pruning | 4,624,962,392,952,718,272 | 57,327,316,898,095,778,834,599,046 |
| 4 | Pruning + compression | 1,577,285,295,120,527,798 | 16,730,121,248,948,800,436,260,989 |

Pruning and compression together reduce workload counts by **73.45%, 76.98% and
65.97%**, and fault-scenario counts by **79.50%, 79.50% and 70.82%** for two, three and
four Sagas. Pruning alone has a much smaller effect on the fault-scenario totals for
three and four Sagas; retained long Sagas contribute many fault/recovery combinations.
Do not imply the reduction is uniform across sets or that these counts prove outcome
preservation. Use the separate matched pruning/compression execution evidence for that.

Counts include all-zero assignments. They describe structural candidates, not a claim
that every candidate has a valid setup or an exact successful runtime execution.
Different workloads remain distinct even if failure masks their differing steps.

## Verification

- Complete generation and materialisation of **249,621 fault scenarios** across
  **18 source-derived comparisons**, including 2/3/4 Sagas, event producers and both
  full and compressed scheduling, agrees exactly with the counter. Selection uses
  source structure and deterministic first/middle/last positions before outcomes.
- Generic Spock comparisons cover fault masking, checkpoints, segment tails and
  anchors, one/two/three event consumers, failures before/at/after emission, separate
  emission sites, duplicate routes, downstream-participant exclusion, strict inputs,
  deterministic ordering and refusal of incomplete totals.
- Independent Python sums of all 74,481 rows per profile agree with every summary.
  Per-set counts are monotone from none to pruning to pruning plus compression.
- Source extraction is the frozen current three-variant Quizzes model; its SHA256 and
  compact-table checksums are in `receipts.json`. No application/test/scoring changes.

Measured counting time after loading the extraction: **80.34 s** without reduction,
**55.90 s** with pruning and **92.77 s** with pruning plus compression, on André's Mac.
The first and third ran concurrently, so these are feasibility observations, not a
controlled performance comparison. JVM heap limit 2 GiB; state limit 2,000,000. No
full workload or scenario catalogue is written for the global counts.

## Artifacts and reproduction

- `summaries.json`: exact totals, configuration and input normalization.
- `*-sets.jsonl.gz`: complete compact per-set evidence, replacing verified redundant
  raw files under target; around 3.2 MB total.
- `quizzes-enumeration-verification.json`: 18 independent generation comparisons.
- `independent-summation.json`: sum and nesting checks.
- `receipts.json`: evidence hashes; `source-receipt.json`: final source/test proof.

Production: enable `verifiers.scenario-catalog.count-fault-scenarios=true` together
with `catalog-write-mode=COUNT_ONLY` and `enabled=true`. Other generation settings
still define input admission, selection and event policy. Counts ignore write caps.
An exceeded state/tuple guard yields INCOMPLETE/null, not a misleading partial total.

Frozen-model runner: compile `verifiers/experiments/generation-comparison/CountFaultScenarios.java`
against verifier classes/dependencies. Arguments: model JSON, new output directory,
maximum Saga-set size, generation strategy, schedule strategy, event-delivery limit,
optional maximum DP states. The companion `VerifyFaultCounts.java` independently
compares complete small generation. Local logs and initial feasibility pilots are in
`verifiers/target/fault-count-2026-09-20/`; aborted pilots are explicitly marked and
are not included in these final numbers.
