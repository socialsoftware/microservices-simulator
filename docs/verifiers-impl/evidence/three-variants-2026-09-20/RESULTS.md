# Three Quizzes variants: current generation counts

The same current analyser, input policy and broad interaction rule were applied to all
three application trees. Inputs come from their existing tests. No Quizzes application code or
tests were changed in this recount; generic parser fixtures were added to dummyapp. Two generic extraction fixes support direct Java
pattern-switch command dispatch and omitted trailing Groovy helper defaults.

| Extracted facts | Quizzes | Full | Full-2 |
| --- | ---: | ---: | ---: |
| Saga definitions | 68 | 43 | 46 |
| Saga types with accepted inputs | 37 | 27 | 27 |
| Accepted inputs | 819 | 337 | 350 |
| Inputs removed by cap | 0 | 0 | 0 |
| Extracted steps | 134 | 93 | 89 |
| Resolved event routes | 18 | 0 | 0 |
| Source setup bindings | 794 | 122 | 0 |

Source setup bindings are extraction records, not successful executions or counts of
executable scenarios. Full-2 still has unsupported preparation expressions, including
enum-valued DTO fields and helper calls involving iteration. Its accepted inputs can be
counted, but their preparation has not been established. Both Full variants contain event
consumers whose direct domain operations do not resolve through the currently supported
consumer-to-Saga route. Zero routes therefore means an extraction limit, not no events.
These limits belong beside comparisons of the extracted models: especially, omitted event
effects must not be presented as additional pruning effectiveness.

## Two Saga types per combination

| Count | Quizzes | Full | Full-2 |
| --- | ---: | ---: | ---: |
| All input combinations | 311,137 | 49,139 | 53,475 |
| After interaction pruning | 136,156 | 25,786 | 24,857 |
| All normal step orders | 7,197,188 | 625,950 | 774,290 |
| Normal orders after pruning | 5,789,338 | 496,114 | 610,368 |
| Normal orders after pruning and segment compression | 620,567 | 66,811 | 56,353 |

The first two rows count selected invocations and inputs, before choosing their order.
The last three also count every order that preserves each Saga's own step order.
They do not expand event placements, injected faults or recovery orders, and are not
counts of executable FaultScenarios. No schedule cap is applied to these mathematical
counts. Singles are outside this table; each combination uses distinct Saga types.

For Quizzes, the previous 565,344 compressed-order count belongs to an older extraction
and pruning implementation. The current replacement is 620,567. This is a snapshot
replacement, not a runtime outcome-preservation result.

## Three and four Saga types

All values are exact integers; the same inputs and rules apply.

| Application | Saga types | All input combinations | After pruning | All orders | Pruned orders | Compressed orders |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Quizzes | 3 | 72,904,145 | 27,551,023 | 886,656,927,870 | 865,530,423,236 | 34,140,339,743 |
| Quizzes | 4 | 11,820,344,542 | 4,739,545,978 | 410,502,636,680,930,952 | 408,865,214,814,764,684 | 69,279,814,207,380,330 |
| Full | 3 | 4,076,149 | 1,708,057 | 6,488,974,140 | 6,059,802,185 | 80,661,897 |
| Full | 4 | 213,039,521 | 90,793,636 | 95,646,448,381,540 | 92,714,797,643,430 | 1,160,758,241,790 |
| Full-2 | 3 | 4,707,948 | 2,042,102 | 12,545,470,216 | 11,870,159,226 | 48,766,200 |
| Full-2 | 4 | 265,811,187 | 136,625,001 | 458,490,208,830,408 | 448,644,946,661,730 | 148,628,865,494 |

Removing many input combinations need not remove a similar fraction of orders: the
remaining combinations can contain longer Sagas with many more interleavings.

## Verification and reproduction

- 75 focused visitor tests pass: 23 command-dispatch tests and 52 Groovy tracing tests.
  New fixtures exercise direct read/write branches, guards, nested switches, multiple
  service targets, helper defaults, explicit overrides and ambiguous overloads.
- Current production selection includes compensation and resolved event effects.
  Compression uses all tuple-applicable anchors, including indirect anchors; it does
  not reuse the old forward-only graph or its minimal connectivity evidence.
- All inputs have empty exact logical-key bindings. Under broad selection, source
  identity can prove equality but not inequality. Selection/anchors are therefore input
  independent within each Saga set; the harness fails if exact bindings are introduced.
  It additionally checks 47,346 alternative input substitutions across all pairs.
- Production grouped accounting independently agrees for all 1,368 Saga pairs using one
  input per Saga. This bounded check has a 5,000-order cap, applied on both sides; the
  main mathematical counts are uncapped.
- 46 structurally stratified small sets were fully enumerated. Counts, in-Saga order,
  absence of duplicates and the complete set of anchor-order projections agree.
  Selection is deterministic by Saga-set hash, before observing compression ratios.
- Independent Python summation of all per-set rows agrees with the summary tables.
- A fresh extraction of all three applications after final code review produces exactly
  the same models; its receipt pins the final visitor sources.

Harness: `verifiers/experiments/generation-comparison/ThreeVariantCounts.java`, sharing
production extraction and enumeration utilities in `GenerationComparison.java`.
Run with Java 21, verifier classes/dependencies on the classpath, arguments
`<applications-root> <application> <new-output-directory>`. Compile both Java files.
`summary.json`, `counts.jsonl` and enumeration/accounting receipts are written to that directory.

Raw extraction/models and classpath: `verifiers/target/three-variants-2026-09-20/`.
Compact summaries, complete compressed per-set rows and enumeration receipts are alongside
this report. `receipt-reference.json` pins the local source/artifact manifest. No Docker
or application execution, cluster compute, performance claim or paper edit was involved.
