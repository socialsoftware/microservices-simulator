# Bounded same-event route comparison

This campaign uses ordinary generated workloads and ScenarioExecutor replay. It compares
selected delivery horizons for one exact source fixture; it does not drain all events,
change the impact formula, mutate production Quizzes code, or infer business harm.

Run from the repository root with Docker available:

```sh
python3 verifiers/experiments/combined-event-deliveries/run.py \
  --output verifiers/target/combined-event-deliveries/NEW_RUN
```

The output directory must not exist. The runner uses the existing Compose executor image
and prepared-build scripts, copies simulator/verifier/Quizzes source into the output,
checks copy hashes, compiles inside Docker, and generates from that frozen Quizzes copy.
Generation enables `max-event-consequences-per-workload=3`, singles, SERIAL, a 10000
workload cap, 1000 inputs per Saga and 20 recovery schedules. No unrelated services are
stopped. Each attempt gets a fresh JVM/H2 state; at most two attempts run simultaneously.

The selection is frozen before outcomes using the exact
`RemoveCourseExecutionQuizAnswerReceiverTest` removal input. The same setup ID must be
used by every selected workload. A case selects an exact route order and fault vector;
if recovery variants exist, the smallest persisted scenario ID is chosen deterministically.

| Case | Selected deliveries | Vector | Question |
| --- | --- | --- | --- |
| none | None | 000 | What remains immediately after successful removal? |
| answer | QuizAnswer | 000 | What changes after that subscriber processes the event? |
| quiz | Quiz | 000 | What changes after the other subscriber processes it? |
| answer-quiz | QuizAnswer then Quiz | 000 | Do both selected reactions remove the observed conditions? |
| quiz-answer | Quiz then QuizAnswer | 000 | Does changing their order alter this result? |
| answer-quiz-trigger-fault | QuizAnswer then Quiz | 001 | Are both masked when removal never emits the event? |
| quiz-answer-trigger-fault | Quiz then QuizAnswer | 001 | Is masking independent of selected delivery order? |
| answer-quiz-missing-tournament | QuizAnswer, Quiz, Tournament | 000 | Is the absent third receiver explicit after earlier deliveries succeed? |

A Tournament is deliberately absent from this existing fixture. The last case must
retain its incomplete execution, earlier persisted effects and INVALID/null complete
score; it is not a successful three-receiver experiment. Generic three-route replay
has separate fixture tests. Source setup can support combinations when one coherent
fixture prepares every receiver; existing provider descriptor selectors remain
singleton-only and are not an automatic route-combination preparation mechanism.

`validate.py --run PATH` reuses the existing report validator and checks source-selected
order, exact shared event ID, distinct action IDs, masks, final object states and score.
Assertions about object IDs/expected Quizzes outcomes belong to this evaluation script,
not the generic detector. `comparison.json` retains report paths/hashes and all eight
attempts. This campaign's expected lifecycle/score contrasts were selected from the
source behavior; selection itself never filters outcomes.

The first build/generation attempt (`run-01`) exposed an inherited Compose environment
variable incompatible with Spring Boot configuration. The generator launcher now unsets
`SPRING_PROFILES`/`SPRING_PROFILES_ACTIVE`, like the existing execution launcher. No
scenario ran in that first launch; its logs remain. `run-02` is the eight-attempt campaign.
