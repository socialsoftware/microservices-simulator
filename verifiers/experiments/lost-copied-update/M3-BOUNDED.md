# Bounded generated qualification

Run from the repository root. The generation command preserves the qualified source-test
filter, two-saga scope, type-only fallback, fixed caps, deterministic seed, source-derived
setup, and immutable image from the frozen runtime descriptor:

```sh
bash verifiers/experiments/lost-copied-update/generate-bounded-package.sh \
  --runtime verifiers/target/lost-copied-update/integration-03/runtime.json \
  --prepared-build verifiers/target/empty-event-delivery/run-01/prepared-build \
  --source-test applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/tournament/UpdateTournamentTest.groovy \
  --output verifiers/target/lost-copied-update/generated-positive-regeneration \
  --source-class pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.UpdateTournamentTest \
  --source-method 'update topic and tournament successfully' \
  --saga pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas.UpdateTopicFunctionalitySagas \
  --saga pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas \
  --max-workloads 100 --max-input-variants 10 --max-schedules 40 \
  --recovery-cap 100 --seed 9092026 --allow-type-only-fallback true
```

The checked-in case file points at the retained final package and frozen runtime. Replay all
three accepted cases, or add `--case forward-stale` to select one:

```sh
python3 verifiers/experiments/lost-copied-update/qualify_bounded.py \
  --config verifiers/experiments/lost-copied-update/m3-bounded.json \
  --output verifiers/target/lost-copied-update/generated-positive-requalification
```

The type-only fallback is explicit because static aggregate-key evidence does not resolve the
collection-valued `topicsAggregateIds` binding. Runtime evidence is required to establish the
concrete shared Topic identity.
