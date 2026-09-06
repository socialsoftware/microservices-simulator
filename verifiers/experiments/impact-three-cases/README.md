# Four-run impact experiment

For the domain model, ordinary user stories, and step-by-step explanation of all four
runs, start with [the canonical handbook](../../../docs/verifiers-impl/current-state.md#understanding-impact-through-quizzes).
This directory owns the runnable research method and detailed technical evidence.

Approved on 2026-09-05 as a bounded research experiment. The purpose is to determine
which observed effects can support a useful final-state impact measure before changing
ImpactV1. No production score, source analysis schema or application behavior is changed.
No LLM inference forms part of this method.

## Question and cases

Can the same small detector identify surviving references to objects removed during
recovery or partial execution, without flagging successful intermediate visibility as
damage? The initial three examples are extended by one necessary exposure-only control:

- CreateTournament generates a Quiz, fails and compensates before StartQuiz reads it.
- StartQuiz caches that Quiz, compensation deletes it, and StartQuiz completes.
- RemoveTournament alone deletes its Quiz, faults before deleting its Tournament, and
  executes its available recovery. There is no second Saga in the measured attempt.
- StartQuiz reads an unfinished CreateTournament's Quiz and both Sagas succeed.

The first three reproduce the behavior and controlled Saga API calls in the existing
Quizzes recovery-window tests; the single-Saga case isolates the remaining effect before
the second participant in the older Remove/Add test. The fourth applies the user's
agreed requirement that intermediate visibility alone contributes no damage.

These are application-side research runs using current workflow APIs. They are not
new normal generated FaultScenario executions, a full serializability comparison, or a
qualification of the 665 static setup candidates. The harness supplies the intermediate
Quiz ID directly to generic StartQuiz. Normal Tournament solving uses a distinct workflow;
ordinary UI discovery/reachability of this intermediate Quiz has not been demonstrated.

## Observation versus interpretation

`ImpactExperiment.java` owns application setup, controlled actions, exact fault injection
and typed observations. It exports persisted lifecycle/version facts and two concrete
relationships through their actual getters: QuizAnswer→Quiz and Tournament→Quiz.
This observation adapter is application-specific. It does not declare that either
relationship must always target a live object.

`analyze.py` consumes those facts without Quizzes class names, case IDs, expected results
or property-name semantics in its detector. Its rule is:

> An object that is ACTIVE at the observation horizon retains a reference to an object
> that became DELETED during the observed attempt and remains DELETED at the horizon.

It reports distinct candidate source objects, the deletion interval, the actor responsible
for that interval and any observed prior read by the actor that created the source.
It can therefore identify a candidate in the single-Saga partial-removal case without
requiring a cross-Saga read dependency.

Exposure is reported separately when an actor reads an object created by another actor
whose successful finalization has not yet been observed. Raw aggregate-access events
contain an object ID and type but no exact read version. Where the harness additionally
observes the returned Quiz DTO version and the stored reference version, the analyzer
can test equality with the producer's observed creation version. This is supplemental
probe evidence, not a claim that existing raw traces already contain those fields.

The detector deliberately returns `impactScore: null`. It does not know whether a
reference is live-required or historical. An intentional historical-reference control
demonstrates that the same structural finding could be legitimate. Zero candidates
means this particular pattern was absent in the observed slice, not universal safety.

## Evidence boundary

- Every case uses a fresh JVM/Spring/H2 context. The build uses current repository source.
- Setup facts and events are outside the measured trace. Setup-created pending events
  are cleared and the empty initial baseline is verified.
- Snapshots are taken through the persistence layer after controlled action intervals.
  The observed object slice is explicit; it is not a whole-application consistency scan.
- Observation queries do not contribute read events to the measured trace.
- Measured pending events are retained. A nonempty queue prevents a settled global
  outcome claim; an empty queue establishes only the selected local replay horizon.
- Lifecycle, semantic-lock state, execution outcome, candidate count and invariant
  rejections remain separate facts.
- Current-time fixture dates and raw event IDs/durations can vary. Repeatability concerns
  normalized detector findings and exact identity/version relationships, not raw log bytes.

## Run

From the repository root, using the existing scenario-executor image:

```sh
docker compose run --rm --no-deps --pull never -T --entrypoint bash scenario-executor /verifiers/experiments/impact-three-cases/run.sh
python3 verifiers/experiments/impact-three-cases/analyze.py verifiers/target/impact-three-cases/run-1/*.json --output verifiers/target/impact-three-cases/analysis-run-1.json
python3 verifiers/experiments/impact-three-cases/analyze.py verifiers/target/impact-three-cases/run-2/*.json --output verifiers/target/impact-three-cases/analysis-run-2.json
python3 -m unittest discover -s verifiers/experiments/impact-three-cases -p 'test_*.py' -v
```

The runner performs all four cases and repeats the split-reader and successful-overlap
cases. Raw JSON, full logs and derived findings live under
`verifiers/target/impact-three-cases/`. See RESULTS.md for the actual measured outcome,
remaining semantic decision and reproducibility evidence.

## Behavioral follow-up

`run-behavior-probes.sh` builds once and runs six independent fresh-JVM probes: affected
and healthy Tournament reads, Tournament-then-Quiz read sequences, and student answer
submissions. Run it through the same Docker entrypoint as run.sh. Outputs default to
`verifiers/target/impact-behavior-probes/` and do not replace the original four-run reports.
`ImpactExperiment --behavior-probe PROBE_ID OUTPUT_PATH` embeds the original scenario
report before recording the separate continuation. See RESULTS.md for matched controls,
actual outcomes, the broken answer-submission baseline and interpretation limits.
