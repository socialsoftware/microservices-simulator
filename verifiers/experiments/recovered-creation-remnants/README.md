# Reassess recovered-creation remnants

## Approved direct brief

The user approved excluding a new creation logically deleted during recovery from the
residual score, while retaining active dependencies, unremoved creations, preexisting
changes and unknown evidence. Implement directly in the current checkout, test the
assessor/executor/independent read diagnostic, then reassess retained evidence and update
the canonical contract. No new issue package or implementation approval is needed for
this bounded change. The meeting note and original experiment reports are preserved.

## Result

259 JDK21 Spock tests passed: 22 assessor, 195 executor and 42 Saga-read diagnostic tests.
The current Java assessor reassessed 188 retained report pairs: 175 space-map attempts,
two Course counter controls and eleven original ImpactV2 qualification runs. Ten scores
changed from 1 to 0, all w07/w08 recovered QuizAnswer creations. Eight are discovery
attempts and two are repetitions. Other category results and all assessment coverage
statuses, candidates and unknown reasons are unchanged.

The three canonical w07 positive sequences and three w08 positive sequences now score
zero. Across the retained space-map evidence: 129 COMPLETE zeros and 46 INVALID/nulls.
The counter remains positive after failed removal; a surviving active Tournament still
scores for its deleted Quiz dependency, alongside the preexisting Quiz residual.
This is **offline reassessment, with zero new application executions**. It validates
changed interpretation of the collected facts; it does not requalify current runtime
collection, repair invalid event scenarios or rerun the search algorithms.

## Reproduce

`Reassess.java` calls the actual production assessor, not a duplicate Python predicate.
It expects an input JSON array of `{name, execution, impact}` entries with absolute paths
to the original execution report and ImpactV2 report. Output must be a new directory.
The input/output hashes and all old/new categories are written to `summary.json`.
Original reports are never overwritten. The tool validates joined attempt/workload/scenario
identity; it is an experiment utility for these trusted retained reports, not a new
public importer or a substitute for the normal package validator.

Build the current verifier and its current simulator dependency in an isolated JDK21
source/cache environment. Run these tests from the copied `verifiers/` module:

```sh
mvn -Dtest=ImpactV2AssessorSpec,ScenarioExecutorSpec,SagaReadExposureSpec test
```

Compile `Reassess.java` with that build's `target/classes` and dependency classpath,
then run `pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.Reassess`
with the input manifest and new output directory as its two arguments. Do not put a
historical verifier JAR before the freshly compiled assessor on the classpath.

Retained local files:

- `verifiers/target/recovered-creation-remnants/inputs.json`: the exact 188 input pairs;
- `reassessment/`: derived reports and full old/new summary;
- `surefire-reports/`, `tests.log`, `source-hashes.json`: validation proof;
- `classpath.txt`: the exact original validation classpath (local, not portable).

[Compact comparison](../../../docs/verifiers-impl/evidence/recovered-creation-remnants-2026-09-07/comparison.json)
and [proof](../../../docs/verifiers-impl/evidence/recovered-creation-remnants-2026-09-07/proof.json)
are retained in Git. Original inputs remain at their recorded locations. Preserve local
archives before cleaning targets. Historical campaign scores are not rewritten; the
[decision](../../../docs/verifiers-impl/decisions/2026-09-07-recovered-creation-remnants.md)
and current handbook own the revised interpretation. The report schema is unchanged.
