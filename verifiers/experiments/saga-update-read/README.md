# Reading a Tournament update that is subsequently compensated

A bounded application-side experiment approved in conversation on 9 September 2026.
It executes the real `UpdateTournamentFunctionalitySagas` and
`FindTournamentFunctionalitySagas` through the controlled workflow APIs. The original research mode establishes the application witness. An optional integrated
mode submits the same measured facts to the production diagnostic; neither mode repairs
Quizzes or changes persistent-object impact scoring.

## Question and controls

Prepare one existing Tournament with its Quiz through normal application operations.
A changes the start time from 15:00 to 16:00, the end time from 15:55 to 17:00,
and the question count from two to three, adding the third prepared topic. B calls the
existing find functionality on that same setup-created Tournament ID.

| Case | B's position | A's outcome |
| --- | --- | --- |
| `success-between` | After the Tournament update, before the Quiz update | Complete and commit |
| `fault-between` | After the Tournament update, before the Quiz update | Assigned pre-body fault at `updateQuizStep`; recover every checkpoint |
| `fault-before` | Before A starts | Same assigned fault and recovery |
| `fault-after` | After A finishes recovery | Same assigned fault and recovery |

Run each once with local messaging serialization off and once with it on: four
experimental histories, eight fresh JVM/Spring/H2 executions. These are transport
controls, not eight distinct anomaly types or statistical repetitions.

The fixture uses future dates on 1 January 2030 and a course ending the following day.
The application clock itself is not replaced. Events from setup are cleared, and replay
mode suppresses background delivery. The measured scenario schedules no event deliveries.
The controlled fault uses the actual fault-vector boundary API. The harness runs both
explicit compensation and implicit semantic-state rollback, then checks that no recovery
checkpoint remains. Successful A and B executions call the real workflow finalization API.

## Observation and proof

The harness records:

- Ordered committed writes, writer/phase attribution and successful gateway response
  metadata using the existing framework observation interfaces.
- The DTO B actually retains, copied immediately after its return and again at the horizon.
- Transactional database snapshots at the initial state, before/after the update,
  at the read, after the fault/Quiz update, after each recovery checkpoint and at the horizon.
- Exact fault identity, recovery modes, action outcomes, observer failures and raw events.

The selected DTO fields explain what was returned. Full application projections from
committed-write evidence allow inspection of recovery differences outside that selected
projection. Neither receiving a DTO nor retaining it demonstrates a subsequent business
decision or persistent harm. The original research mode does not invoke an assessor. The integrated mode invokes
`SagaReadExposureCollector` and its production assessor, saving a separate sidecar plus
the exact source/action contract. The independent application verifier checks the actual
DTO and database observations before comparing the production verdict.

## Integrated diagnostic qualification

```sh
python3 verifiers/experiments/saga-update-read/qualify-integrated.py \
  --output verifiers/target/saga-update-read/integrated-NN \
  --control-package /absolute/retained/ordinary-control-package \
  --jol /absolute/jol-cli-0.17-full.jar
python3 verifiers/experiments/saga-update-read/validate-integrated.py \
  verifiers/target/saga-update-read/integrated-NN
```

The runner verifies the prepared runtime, permits only the explicitly frozen
`SagaRead*.java` production overlay, and compiles those current detector classes ahead of
the unchanged prepared classes on the runtime classpath. Every other recorded production
source and every prepared binary/dependency must still match. This avoids recompiling an
unchanged simulator/application or silently running the old detector. Frozen harness and
overlay sources, package hashes, all logs and class files are retained per run.

It executes the eight update controls, a creation-positive and failed-read creation control,
and a normal ScenarioExecutor baseline control that persists the current sidecar schema.
The ordinary control is setup-backed FindQuiz; it is not a generated positive for the
new UpdateTournament/FindTournament pair. The integrated update source contract declares
occurrences/checkpoints before measurement and maps measured action outcomes to the same
report types used by the ordinary executor.

## Original research-only runner

This strict runner requires the unchanged original qualified production build. After a
production diagnostic change use the integrated runner above; a drift rejection here is
intentional.


```sh
python3 verifiers/experiments/saga-update-read/run.py \
  --output verifiers/target/saga-update-read/run-NN
python3 verifiers/experiments/saga-update-read/validate.py \
  verifiers/target/saga-update-read/run-NN
```

The runner reuses `verifiers/target/empty-event-delivery/run-01` only after verifying all
791 recorded production sources and all 1,777 recorded build/dependency artifacts.
It checks them again after execution. If they differ, it stops; rebuild and qualify the
prepared runtime before reusing this experiment. The retained build also contains the
existing experiment prerequisite provider; this harness prepares its own inputs through
application operations and does not invoke generated scenario setup.

Only the new harness is compiled. Docker uses the recorded image ID, two CPUs, 3 GiB,
no network, read-only source/build mounts, and a new output directory. Every run retains
its command, source/tool hashes, JVM version, logs, exit status and report hashes. Failed
attempts remain available. This is a hand-scheduled controlled experiment, not evidence
that the generator or ordinary ScenarioExecutor already emits this pair.

[Results and next steps](../../../docs/verifiers-impl/evidence/saga-update-read-2026-09-09/README.md).

[Implemented detector and integrated qualification](../../../issues/2026-09-09-compensated-update-read/HANDOFF.md).

[Generated package and ordinary-executor follow-up](generated/README.md).
