# Migrated JMeter thesis cases

The impairment module no longer reads the CSV behaviour files. Delays are now configured per topology through
`POST /behaviour/inject`, and fault injection is gone. The JMeter and Groovy tests built on those CSVs are being
retired. The numbered JMeter tests never used the CSVs: they order their steps with `/scheduler/stop|start`,
`/versions/decrement` and the `/tournament/process/*` endpoints. They are ported here as single-user Locust
scenarios. Each port keeps the JMeter test's steps, payloads, fixed sleeps and assertions.

## Running

```bash
cd applications/quizzes
TX_MODE=sagas docker compose up -d --build quizzes-local   # or TX_MODE=tcc

cd src/test/locust
.venv/bin/python run_tests.py migrated --tx-mode sagas      # must match the TX_MODE the app was started with

# a single scenario
cd tests && ../.venv/bin/python -m locust -f migrated/<file> --headless -u 1 -r 1 --run-time 180s
```

- **Fresh app.** Run the suite on a freshly started app, and restart it before running the suite again. The JMeter tests always ran on a fresh app; their fixed acronym forced it. On a long-lived app, the scenarios fail because of the two application issues described below.
- **Profiles.** `--tx-mode` skips the scenarios that don't hold under that profile (see the table).
- **Pass criteria.** A migrated scenario only passes if it reports `Test Succeeded!`. It quits Locust itself, and `--run-time` is only a safety cap. The default suite's `--iterations` flag belongs to locust-plugins, which isn't installed. Locust 2.46 rejects that flag, and the default runner reads the resulting error as a pass.
- **Scheduler state.** Every scenario stops the event scheduler before `AppUtils.start()`. `/scheduler/start` registers the event polling again, so on an app whose scheduler is already running, events would otherwise be polled twice. Like the rest of the suite, a scenario ends with `AppUtils.stop()`, which leaves the scheduler stopped.

## Comparing with the JMeter tests

`compare_with_jmeter.sh` runs each numbered `.jmx` and then its Locust port. It restarts `quizzes-local` before every
run, so each run starts on an empty database, which the JMeter tests need. It then prints whether each pair passes or
fails alike, with the failed JMeter samples and Locust checks:

```bash
cd applications/quizzes/src/test/locust/tests/migrated
./compare_with_jmeter.sh --start --tx-mode sagas      # builds and starts the stack, then compares all 10
./compare_with_jmeter.sh --tx-mode tcc --tests 8-6,8-7  # app already running with tcc, compare a subset
```

- **Docker access.** If `docker` isn't accessible to your user, the script uses `sudo docker` and asks for the password once.
- **Results and exit code.** Outputs go to a new `/tmp/jmeter-vs-locust-<mode>-<timestamp>/` folder, or `--results-dir`. The script exits with 1 when JMeter and Locust disagree on a test.
- **Help.** `--help` lists every option.

## Mapping

| JMeter (`jmeter/tournament/thesis-cases/`) | Locust (`tests/migrated/`) | Profiles |
|---|---|---|
| `5a-updateStudentName-addParticipant-processUpdateNameEvent` | `5a_update_name_then_add_participant_test.py` | sagas, tcc |
| `5b-addParticipant-updateStudentName-processUpdateNameEvent` | `5b_add_participant_then_update_name_test.py` | sagas, tcc |
| `5c-updateStudentName1-addParticipant-updateStudentName2-processUpdateNameEvent` | `5c_add_participant_concurrent_update_name_test.py` | sagas, tcc |
| `5d-addParticipant1-updateStudentName-processUpdateNameEvent1-addParticipant2-processUpdateNameEvent2` | `5d_update_name_processed_then_concurrent_add_participant_test.py` | sagas, tcc |
| `8-5-update-tournament-concurrent-intention-pass` | `8_5_concurrent_update_tournament_test.py` | sagas, tcc |
| `8-6-add-participant-concurrent-update-execution-student-name-processing-ends-first` | `8_6_add_creator_concurrent_update_name_processed_first_test.py` | tcc |
| `8-7-add-participant-concurrent-anonymize-event-processing-processing-ends-last` | `8_7_add_creator_concurrent_update_name_processed_last_test.py` | tcc |
| `8-8-update-execution-student-add-participant-process-event-add-participant` | `8_8_update_name_then_add_creator_after_event_test.py` | sagas, tcc |
| `8-9-add-participant-concurrent-anonymize-event-processing-processing-ends-first` | `8_9_add_participant_concurrent_anonymize_processed_first_test.py` | sagas, tcc |
| `8-10-concurrent-delete-tournament-add-participant` | `8_10_concurrent_remove_tournament_add_participant_test.py` | sagas, tcc |

The Locust files are named after what each test does. Three JMeter names don't match their content: 5c has a single name
update, 5d has a single join, and 8-7 processes an update name event, not an anonymize. The numbered tests were
written for TCC: `scripts/benchmark-deployments.sh` reserves numbered files for `--tx-mode tcc`.

8-6 and 8-7 are TCC-only. They assert a conflict that the version decrement creates. In 8-6 the creator's join is
rejected; in 8-7 the event processing is rejected. Sagas has no such conflict and applies both operations to the
latest state. Run on sagas, 8-6 fails because the join succeeds and the creator becomes a participant. 8-7 fails
because the join is rejected and the event processing succeeds. Both also failed when the JMeter tests were run on sagas.

## Verification against JMeter (2026-10-09)

Each `.jmx` was run with the benchmark script's pass rule (JMeter exit 0 and no failed sample), on a freshly started
app per test. The Locust ports then ran once on a freshly started app. Both used the `quizzes-local` stack built from
this branch.

| Test | sagas: JMeter | sagas: Locust | tcc: JMeter | tcc: Locust |
|---|---|---|---|---|
| 5a, 5b, 5c, 5d, 8-5, 8-8, 8-9, 8-10 | pass | pass | pass | pass |
| 8-6 | fail: join returns 200, participants not empty | fail: same checks | pass | pass |
| 8-7 | fail: join 400, event processing returns 200, participants empty | fail: same checks | pass | pass |

One Locust setup failed on a database serialization conflict (see below). That is why the setup is now retried.

## Known application issues

These show up when the suite runs on an app that already ran it. They are in the application, not the tests: the
JMeter tests trigger them too.

- **The 8-9 anonymize event is redelivered forever.**
  - `EventHandler.getEventSubscriptions` (simulator) reads the subscriptions from the subscriber's latest *ACTIVE* version. Once the anonymize event inactivates the tournament, the event looks unprocessed again.
  - `TournamentService.anonymizeUser` has no already-processed guard, unlike `removeCourseExecution`. So every poll throws `Cannot update aggregate N because it is INACTIVE`, which aborts the rest of that handler's poll.
  - In a later run, `/tournament/process/anonymize` therefore fails and 8-9 fails.
- **Event polling slows down as data accumulates.**
  - `EventHandler.getAggregateIds` and `getEventSubscriptions` load every version of every aggregate (`findAll()`), once per subscriber.
  - After a few suite runs, one polling cycle took about 8 s, so the fixed waits of 5b (10 s) and 8-8 (20 s) were exceeded.
- **Serialization conflicts.**
  - Events left pending by one scenario are processed while the next scenario creates its data, and PostgreSQL's serializable isolation can abort either transaction.
  - `create_tournament_base_data` retries with new data. The scenario steps are not retried.

## Differences from the JMeter tests

- **Setup data.** The setUp requests, their order and their bodies are the JMeter ones (`AppUtils.create_tournament_base_data`), with two exceptions:
  - The acronym gets a per-run suffix (`TESTBLCM_<suffix>`). The application rejects a second execution with the same acronym and academic term, so the JMeter tests needed an app restart before each run.
  - The tournament dates are a correct now + 1 day and now + 2 days in UTC. The JMeter tests format them with a 12-hour clock (`hh`) in local time and append `Z`, so their real offset varies between about 12 hours and 1 day.
- **Every step is checked.** JMeter counted any non-2xx sample as an error, even without an assertion, so here each step must succeed unless the JMeter test asserted that it fails.
- **Assertions.** The JMeter tests used JSONPath deep scans; the ports use exact checks.
  - `participants[0].name == X` becomes "the participants are exactly `[X]`". These scenarios never have more than one participant.
  - 8-9 `$..state == INACTIVE` checks the tournament's own `state`. Anonymizing the creator inactivates both the tournament and the creator.
- **Kept on purpose:**
  - 8-8's first join is sent without `executionAggregateId` and expects 400 from request validation.
  - 8-5's updates send only `aggregateId` and `numberOfQuestions`. `AppUtils.update_tournament` would also change the start and end times, which would add intentions to the concurrent updates.
- **Dropped:** 8-9's anonymize request body, which the endpoint ignores, and 5d's disabled 1100 ms timer.

## Deletion checklist

Not executed yet.

- [ ] `applications/quizzes/jmeter/`: every `.jmx`, `msdb_dump.sql` and `jmeter.log` (this includes the non-numbered sagas tests, which load `jmeter/faults` CSVs)
- [ ] `tests/migrated/compare_with_jmeter.sh` and the "Comparing with the JMeter tests" section above. They only make sense while the `.jmx` files exist.
- [ ] `applications/quizzes/jmeter.log`
- [ ] `src/test/resources/jmeter/`
  - `delays/` is not referenced anywhere.
  - `faults/` is only loaded by the non-numbered `.jmx`.
- [ ] `src/test/groovy/.../quizzes/sagas/behaviour/`: 13 test files
- [ ] `src/test/resources/groovy/`: all 17 CSV directories
- [ ] The Groovy tests outside `behaviour/` that load CSVs through `loadBehaviorScripts()`:
  - [ ] `sagas/coordination/answer/StartQuizCompensationTest.groovy`: fault CSV. Faults are no longer injected, so it can't pass.
  - [ ] `sagas/coordination/tournament/CreateTournamentCompensationTest.groovy`: fault CSV. Same.
  - [ ] `sagas/coordination/tournament/CreateTournamentAsyncTest.groovy`: delay-only CSV. Its assertions don't depend on the CSV.
  - [ ] `sagas/coordination/question/UpdateQuestionTopicsAsyncTest.groovy`: delay-only CSV. Same.
- [ ] Follow-ups once these are gone:
  - `QuizzesSpockTest.loadBehaviorScripts()` and `mavenBaseDir` have no callers.
  - Stale JMeter references in `scripts/benchmark-deployments.sh`, `README.md` (Benchmarking and Performance Tests), and `guides/run-maven.md` (Running JMeter tests).

**Kept:** the other 33 Groovy tests under `sagas/coordination/` and `causal/`, plus the base and bean configuration
classes. None of them loads a CSV. `AddParticipantAsyncTest` and `SolveQuizAsyncTest` only call the deprecated
`impairmentService.cleanUpCounter()`, which reads no file.
