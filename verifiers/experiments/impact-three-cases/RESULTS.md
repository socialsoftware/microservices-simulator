# Results: surviving effects after recovery

Date: 2026-09-05. Execution: Sol Medium; integration/review: Astra; independent
detector review: Luna Max. Scope: the approved three-example experiment plus an
exposure-only successful control. No production impact behavior changed.

For the reader-facing domain and run sequences, see
[the canonical handbook](../../../docs/verifiers-impl/current-state.md#understanding-impact-through-quizzes).
The harness uses generic StartQuiz with a known Quiz ID; it does not establish that the
ordinary Tournament solve UI exposes an unfinished Tournament's Quiz.

## Outcome

One deterministic, application-name-independent rule distinguishes the two observed
surviving-reference cases from both controls. Existing invariant-rejection telemetry
is zero in all four cases. The rule also identifies the single-Saga partial-removal
case, where no cross-Saga read dependency exists.

| Case | Observed actors | Intermediate exposure | Distinct candidate objects | Guard rejections | Pending events at end |
| --- | --- | ---: | ---: | ---: | ---: |
| Compensation before StartQuiz can consume Quiz | A compensated; B rejected because Quiz is absent | 0 | 0 | 0 | 0 |
| StartQuiz caches Quiz, compensation deletes it, StartQuiz succeeds | A compensated; B succeeds | 1 | 1 QuizAnswer | 0 | 0 |
| RemoveTournament alone fails after deleting Quiz | A performs its available recovery; no B | 0 | 1 Tournament | 0 | 0 |
| StartQuiz reads unfinished Quiz, both Sagas succeed | A succeeds; B succeeds | 1 | 0 | 0 | 0 |

All observed final Quiz/Tournament/QuizAnswer semantic locks are NOT_IN_SAGA. Two
setup-created events were cleared before each measured run; measured event queues were
not cleared or drained. They were empty at the final local replay horizon in every run.
This is not a claim about unobserved remote delivery or every aggregate in the application.

## What the evidence proves

In the split-reader execution:

1. CreateTournament produces Quiz 9 at version 18.
2. StartQuiz's returned DTO identifies Quiz 9 at version 18.
3. CreateTournament compensation leaves Quiz 9 DELETED at version 19.
4. StartQuiz succeeds and leaves QuizAnswer 10 ACTIVE, retaining Quiz 9/version 18.

The raw aggregate-access event proves object access but contains no read-version field.
The cached DTO probe and persisted AnsweredQuiz reference supply the exact version
chain. The evidence therefore improves on a guessed dependency based only on shared IDs.
These supplemental observations are test/research support, not new runtime telemetry.

In the single-Saga case, the initial persisted snapshot already contains active
Tournament 10 and Quiz 9. A's removeQuizStep leaves Quiz 9 DELETED at version 20, the
assigned removeTournamentStep fault fires before its body, and the only available
recovery releases the Tournament lock. Tournament 10 remains ACTIVE at version 19,
retaining Quiz 9/version 18. It has no participants; no concurrent AddParticipant is
needed to produce this remaining effect.

The successful-overlap control observes the same Quiz 9/version 18 before its producer
finishes. Both Sagas then complete. Quiz 9 remains ACTIVE, and the surviving Tournament
and QuizAnswer reference that same version. The analyzer records the exposure and
reports zero candidates, keeping exposure separate from final-state damage.

## Rule and metric boundary

The rule finds an ACTIVE observed object referring to a target that changed to DELETED
during the attempt and is still DELETED at the final observation. It counts distinct
source objects, not trace events or duplicated references. The same code handles
QuizAnswer→Quiz and Tournament→Quiz; its branches contain no application class names,
case IDs or expected verdicts. The Java observer does use typed application getters to
extract those two relationship facts. Automatic relationship extraction is not built.

`candidateAffectedObjectCount` is now a reproducible measurement for this pattern.
It is **not yet a domain-harm score**. `impactScore` remains null in the research report,
and current ImpactV1 is unchanged.

The unresolved semantic question is precise: **does a surviving reference require its
target to remain live, or may it legitimately preserve a historical snapshot?**
An intentional negative control relabels a reference as historical; the structural
detector still finds it. That demonstrates why reference existence and exact version
provenance alone cannot establish business harm. Likewise, zero candidates means the
observed pattern is absent, not proof of application-wide correctness.

Before promoting this measurement into an impact score, the next bounded check should
examine existing application behavior that uses these surviving objects: whether an
attempt can still be used, whether a surviving Tournament can still provide its quiz,
and which consequences differ from appropriate successful/recovery controls. That
assessment must not silently become a blanket rule that all historical references
must target live objects. The present experiment stops at this explicit decision point.

## Validation and review

- Six fresh JVM/Spring/H2 runs: four cases, plus repeats of the split-reader and
  successful-overlap cases. All harness assertions passed.
- Both repeated cases produced identical normalized analysis results, including object
  IDs, versions, findings, exposures and horizon flags. Raw event IDs, durations and
  current-time fixture dates are not claimed byte-stable.
- Twelve Python tests passed. Controls cover renaming object types, removing the
  surviving consumer, exposure without target deletion, a single actor, an already
  deleted target, missing snapshot coverage, a first-observed tombstone, missing final
  objects, pending events and the intentionally ambiguous historical reference.
- Review caught and fixed the first-observation/creation ambiguity. Snapshots now cover
  all latest persisted objects in a fixed declared three-type slice. The analyzer
  requires authoritative prior absence before inferring creation, observed state change
  before attributing deletion, and a null prior version before calling an observed
  version the creation version. Missing evidence remains explicit.
- Luna's focused re-review found no remaining blocking correctness issue within the
  stated experiment contract. Astra independently validated all six final reports,
  their harness assertions and both normalized repeats; artifact hashes are retained
  in repeatability.json.
- Source identities come from actual application results and persisted rows. No generated
  catalog files, normal executor score, application production code or prior dirty work
  were changed for the experiment.

Commands are in README.md. Raw reports and logs are under
`verifiers/target/impact-three-cases/run-1/` and `run-2/`. Derived evidence:
`analysis-run-1.json`, `analysis-run-2.json`, and `repeatability.json`.

## Earlier all-zero Remove/Add control: rejection diagnosis

This is an analysis of saved execution evidence and the application write path, not a
new execution of that older package. The separate report
`verifiers/target/execution-baseline/reports/pair-00000-execution.json` records:

1. AddParticipant loads the student, adds the participant, and commits successfully.
2. RemoveTournament loads the same Tournament, then completes removeQuizStep.
3. removeTournamentStep fails with `QuizzesException: Aggregate 10 breaks invariants`.
4. The supported runtime recovery rolls back the getTournamentStep lock checkpoint.

The attempt is `PARTIAL_COMPENSATED / DEVIATED`. Its ImpactV1 sidecar records one
invariant rejection at removeTournamentStep, with no assigned fault at any slot.
This schedule completes AddParticipant before RemoveTournament; the rejection does not
require overlapping operations or an injected fault. All-zero means no assigned fault,
not a promise that the application will accept every operation.

The source explains the rejection. TournamentService.removeTournament loads and copies
the Tournament, marks the copy DELETED, and calls registerChanged. The copy retains
participants. Tournament.verifyInvariants first checks invariantDeleteWhenNoParticipants:
a DELETED Tournament must have no participants. The just-committed participant makes
that predicate false. SagaUnitOfWorkService.registerChanged verifies before merging,
so this guard prevents the attempted Tournament deletion from being persisted.

The guard does not itself roll back the earlier Quiz deletion: the Saga orders
removeQuizStep before removeTournamentStep and has no explicit Quiz restoration here.
The old execution report records the completed step and lock recovery, but does not
supply a full persisted final-state snapshot. Distinguish that code/trace explanation
from the fresh standalone-removal experiment's directly observed final state.

Implication for impact design: one rejection can be a correctly enforced local rule,
while earlier effects still need assessment. The invariant count alone cannot distinguish
successful containment from remaining cross-object damage. This case resolves the
previously unexplained all-zero rejection; it does not establish a general harm score.

Source anchors (under `applications/quizzes/src/main/java/.../quizzes/microservices/`):
`tournament/coordination/sagas/RemoveTournamentFunctionalitySagas.java`,
`tournament/service/TournamentService.java#removeTournament`, and
`tournament/aggregate/Tournament.java#verifyInvariants`. The write boundary is
`simulator/src/main/java/.../transaction/sagas/unitOfWork/SagaUnitOfWorkService.java#registerChanged`.

## Behavioral follow-up: three paired probes

Six fresh JVM/Spring/H2 runs compare three existing-operation probes, each with an
affected state and a healthy control. These are diagnostic continuations, not an added
production impact stage. Each report embeds the complete original scenario report and
captures its final snapshot before the probe, then stores probe events, outcomes,
before/after observations and lock evidence separately.

| Probe | Affected state | Healthy control | Supported conclusion |
| --- | --- | --- | --- |
| Find the Tournament | Returns active Tournament 10 with Quiz ID 9 | Same successful result | The surviving Tournament remains retrievable |
| Find the Tournament, then fetch its returned Quiz ID | Tournament read succeeds; findQuiz(9) reports aggregate not found | Both reads succeed; Quiz 9 is active | The public read sequence differs specifically at the referenced Quiz lookup |
| Submit an answer on the student attempt | Fails in getQuizAnswerStep with a null quizAnswer field | Fails at the same step with the same root cause | This probe cannot establish damage caused by compensation; its control is already broken |

The Tournament pair uses the standalone partial-removal state versus the same fixture
with no removal. Both have no participants and use the same relative date offsets.
The second probe is an explicitly declared sequence of two public facades, not a
single existing Tournament endpoint or the SolveQuiz workflow. The Quiz ID is taken
from the public findTournament result. Because enrollment and valid solve-time conditions
were not established, no claim is made that a correctly initiated solve operation fails.

The answer pair uses create-split-start versus create-success-overlap-control. Both
submit quizId 9, userId 4, questionId 7, optionKey 1, sequence 1, and timeTaken 1.
The harness follows the attempt's retained Quiz version 18, then chooses the lowest
question ID and non-null option key from that persisted Quiz and Question 7/version 12. This is
controlled input preparation, not proof of normal UI discovery. Wall-clock timestamps
differ; the fixture uses the same relative offsets. The healthy attempt means an active
Quiz and attempt, not a proven fully valid answer workflow under every timing condition.

Source review and both runtime exceptions identify an existing application defect:
AnswerQuestionFunctionalitySagas.getQuizAnswerStep dereferences its quizAnswer field
before assigning it. No answer was persisted in either run. Fixing that application
path and establishing a valid answer-time control would be separate work before using
answer submission as a discriminating probe. The similar ConcludeQuiz source defect
was noted but not exercised or fixed here.

In all six final reports, the original final snapshot equals the probe's beforeSnapshot,
observed Quiz/Tournament/QuizAnswer snapshots remain unchanged after the probe, pending
queues stay empty, and latest persisted Saga aggregate lock/version observations remain
unchanged, including Question. These checks cover the declared snapshot/lock evidence,
not every possible field or external side effect. The three-type candidate detector
still reports one original candidate in each affected case and none in its control;
its impactScore remains null.

The Tournament DTO's embedded Quiz version is null in these reads. Exact target-version
evidence comes from the original persisted typed reference, not that DTO. The DTO still
supplies the exact Quiz ID used by the read sequence. The application DTO issue is not
repaired or used to infer a version.

The usable matrix now distinguishes three facts: a surviving final-state candidate,
a measured operation difference against a working control, and a probe whose control
fails independently. Only the Tournament Quiz-read sequence shows a difference in this
sample. The user chose to record that demonstrated read failure while keeping domain harm
undecided. Hardcoding an active-Tournament/retrievable-Quiz requirement
would restate an application-specific oracle, not solve generic inference.

Reproduction from repository root:

```sh
docker compose run --rm --no-deps --pull never -T --entrypoint bash scenario-executor /verifiers/experiments/impact-three-cases/run-behavior-probes.sh
```

The six named JSON/log pairs are under `verifiers/target/impact-behavior-probes/`.
No application production code, package contract, original four-run report, or production
impact score was changed. The added research runner and harness mode are separate from
normal generated FaultScenario execution.

Validation: the twelve existing detector tests pass. A separate review checked all six
final probe reports against the harness and found no blocking correctness issues.
`verifiers/target/impact-behavior-probes/validation-summary.json` records report hashes
and the independently checked comparison, snapshot, lock and pending-event facts.
