# Event receiver setup — final handoff

## Outcome

A normal generated Quizzes scenario now prepares a matching QuizAnswer and delivers the
selected anonymization event. The exact Tournament route remains distinct and fails when
its receiver is absent. A pre-emission assigned fault masks delivery. StartQuiz and
LeaveTournament setup examples previously stopped by missing registrations now pass.

Static accounting stays **796 = 576 source setup + 1 without setup + 219 blocked**.
These changes improve route correctness and runtime setup authorization, not that count.
The package contains 944 written workloads including event/provider variants and 2,084
initial fault scenarios. Broader candidate diagnosis is recorded alongside this handoff.

## Changed files and authority

- `ExecutableArtifactWriter.java`: reference authoritative persisted Saga routes; match
  exact participant/step/emission and consumer semantics; reject zero/ambiguous matches.
- `DummyappEventConsequencePackageSpec.groovy`: parsed dummyapp multiple-route subset,
  permutation/byte stability, exact reader round trip, missing and ambiguous route tests.
- `QuizzesSourceSetupActionDispatcher.java`: three explicit typed registrations for
  createQuiz, startQuiz, addParticipant; ten exact method keys total.
- New `QuizzesSourceSetupActionDispatcherTest.groovy`: closed key set, unknown-method
  rejection, metadata and typed delegation; existing application receiver tests unchanged.
- Canonical current-state, roadmap and the event-replay/ordered-setup ADRs; prior runtime
  follow-up now distinguishes resolved method gaps from remaining work.

Astra owns generic export, integration, runtime and docs. Sol Medium implemented the app
slice and independently reviewed export. Astra independently reviewed Sol's slice.
Luna Max diagnosed coverage. Prior dirty work was preserved; no commits, pushes or merges.
See `REVIEW.md` for review boundaries and proof.

## Concrete source fixture

`QuizAnswerEventHandlingTest#AnonymizeStudentEvent updates the student name in the quiz answer`
input `b7f8c6590469784f4d9341b79a9385fb881d3b3b33b1727dedb045fe18dddb79`
uses setup `setup-1d76396ae9f936ceab0ac58d`:
createCourseExecution → createUser → activateUser → addStudent → createTopic →
createQuestion → createQuiz → startQuiz. Setup-level nested question references already
worked; no participant-DTO grammar extension was necessary for this fixture.

The three fresh executions each prepared CourseExecution 2, student 3, Quiz 6 and
QuizAnswer 7; runtime IDs are attempt-local evidence, not generation identity.
The matching route delivered event 2 from CourseExecution 2 to QuizAnswer 7. Existing
application tests assert the resulting anonymous student name. Executor delivery enforces
one eligible subscriber and records the exact bean/handler and subscriber identity.

## Persisted execution selection

Package: `verifiers/target/astra-event-receiver/generated/quizzes-20260904-222711-381/`.

| Case | FaultScenario ID | Observed result |
| --- | --- | --- |
| quizanswer-zero | `48a69d5a4f7c7d1ec7eddcf757b668bbb8af6541a7f6376885db8182b6dcad5d` | `SUCCESS / EXACT` |
| quizanswer-masked | `cfef61a08c53e1014777ba5ac38e10ec70c050789021024e129dd8a516b05816` | `COMPENSATED / EXACT` |
| tournament-missing | `594d65558acc958e049927b8bd8e9372059994cc6912fafe04fb58344d5d2d29` | `UNEXPECTED_EXECUTION_FAILURE / INCOMPLETE` |

Positive route is `anonymizeStudentStep#0/event#0`; Tournament control is
`anonymizeStudentStep#0/event#0-route#1`. The masked event has no receipt; the missing
Tournament receiver reports `SELECTED_SUBSCRIBER_NOT_FOUND` with its exact handling class.
All three cleared one setup event and proved an empty pending-event baseline. No ImpactV1
output was requested; this qualifies replay behavior, not a new impact discriminator.

Two isolated setup preflights passed in 29.76 seconds:
- StartQuiz: `bf2d5e375a8ee32fabf5db41e05aa16db54274c9bc9fec75b30f331f1eb133bc`,
  one pending event cleared.
- LeaveTournament: `736592be74b4c37cbecd6fa6070e54da4c9b30f2c75cd22ffc529b784c886ba2`,
  three pending events cleared.

## Verification and reproduction

- Verifier `mvn -q test`: **778 tests, zero failures/errors/skips**.
- Focused export class: four cases pass, included in the full run.
- Sol snapshot: simulator `mvn -q install`; Quizzes
  `mvn -q -Ptest-sagas test -Dtest=QuizzesSourceSetupActionDispatcherTest,QuizAnswerEventHandlingTest`:
  **5/5 pass** (two dispatcher and three existing application features).
- Two ordinary generations: all nine package files byte-identical. Package hashes remain
  unchanged after runtime. Selected scenarios are existing persisted records, not edits.
- Docker qualification uses fresh JVM/Spring/H2 for each attempt, current compiled source,
  the production reader/executor, and the production preflight parent with a qualification-
  only exact subset selector. No product CLI or package grammar was changed for the proof.

Artifacts under `verifiers/target/astra-event-receiver/`:
`full-verifier-tests.log`, `route-tests.log`, `generation.log`, `repeated-generation.log`,
`determinism.json`, `selected-scenarios.json`, `qualification-summary.json`,
`method-preflight.json`, the three named execution JSON/log pairs, `docker-qualification.log`,
`run-qualification.sh`, and the starting dirty-state snapshot.

Generation uses the existing ScenarioGeneratorApplication with application root
`../applications`, target `quizzes`, catalogue enabled, max Saga-set size 1, max inputs
1000, max written workloads 5000, recovery cap 1, dynamic enrichment disabled.
Run from the repository root:

```sh
docker compose run --rm --no-deps --pull never -T --entrypoint bash scenario-executor \
  /reports/astra-event-receiver/run-qualification.sh \
  /reports/astra-event-receiver/generated/quizzes-20260904-222711-381/scenario-catalog-manifest.json
```

The initial driver attempted two workload IDs from an older intermediate package and
was correctly rejected before runtime. It now selects current IDs by the exact source
inputs; the first failed attempt is preserved as `docker-initial-stale-selection.log`.

## Limits and discoveries

The route projection bug was a necessary repair exposed by qualification, independently
confirmed by Astra and Sol. Earlier packages can preserve exact class fields while still
referencing the wrong route; regenerate them before making intended-consumer claims.
The schema and deterministic model IDs are unchanged. No generic consumer setup inference,
cross-test state unification, full 577 preflight, broad replay, new impact metric or search
was added. Method authorization is checked coverage, not proof every candidate succeeds.
