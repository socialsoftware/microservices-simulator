# Results: update-impact paired experiment

Run on 2026-09-06 from source revision `36784346d1f5e988ad612132b6ed9f5db344ee78`. Four isolated JVM reports passed their runtime checks, and `validate.py` passed provenance and cross-pair checks. Exact report hashes are in `verifiers/target/impact-updates/artifact-hashes.txt` and `validation-summary.json`.

Independent validation reproduced both patched target-file hashes and checked the paired inputs, saved event identity, recovery results and current original source hashes; its evidence is in `verifiers/target/impact-updates/root-validation.json`. Independent review found no blocking issue in the final reports, harness or bounded documentation claims. Provenance covers the declared target patches and recorded sources, not a complete clean-tree manifest of every copied module file.

## Question update: current defect and diagnostic control

The public Question update produced persisted event 4 for Question 7. The Question aggregate changed from version 10 to 21 with title/content `UPDATED QUESTION TITLE` / `UPDATED QUESTION CONTENT`; the later published event carried publisher version 22 and the same payload. These two versions are separate increments and are reported separately.

In the current build, delivery selected Quiz 10 and returned successfully twice. Each delivery emitted the expected command and a Quiz READ, but no Quiz WRITE. After both deliveries, durable Quiz version 19 and its embedded Question 7 remained at version 10 with `Question 1` / `Content 1`. The exact same persisted event therefore remained eligible at the measured second-delivery horizon while producing no durable receiver progress.

In the matched copied-build control, the sole change was:

```java
unitOfWorkService.registerChanged(newQuiz, unitOfWork);
```

The first delivery wrote Quiz 10, whose embedded Question 7 became version 22 with the event payload. A second request for the same event failed with exact replay reason `SELECTED_SUBSCRIBER_NOT_FOUND`, showing that the subscriber version had advanced beyond that event. This supports a generic measurement based on eligible event identity, successful handler outcome, affected aggregate READ/WRITE evidence, and durable receiver projection/version progress. Raw stored-event count alone is not a pending-event measure.

## Tournament update: existing compensation and controlled mutant

Both conditions used the identical request for Tournament 11: question count 3, topics `[4, 5, 6]`, start `2026-09-06T00:00:21`, and end `2026-09-06T01:00:21`. The initial selected projection was version 20, count 2, topics `[4, 5]`. The forward `updateTournamentStep` persisted version 21 with the exact request. An assigned `FaultVectorInjectedFaultException` then stopped `updateQuizStep` before its body; Quiz 10 remained version 19 with two questions.

Normal recovery executed explicit compensation for `updateTournamentStep` and implicit rollback for `getOriginalTournamentStep`. The final Tournament was a newer version 22 whose selected business projection returned to count 2, topics `[4, 5]`, and the original domain dates; affected Tournament and Quiz locks were `NOT_IN_SAGA`. This proves restoration of the selected projection, not byte equality.

The copied-build mutant suppressed only the restoring `UpdateTournamentCommand`. Recovery still reported the same checkpoint execution modes and released the observed locks, but the final Tournament remained version 21 with count 3, topics `[4, 5, 6]`, and the requested dates. Quiz stayed unchanged. This is a controlled missing-compensation residual and not a current Quizzes bug.

## Limits

- Each observation uses one isolated execution with no concurrent writer; it does not establish frequency or general lost-update behavior.
- State comparisons cover the explicitly reported Question, Quiz, Tournament, topic, date, reference, version, and lock fields rather than every application row or framework timestamp.
- Repeated eligibility is established through the replay coordinator's subscriber selection. Persisted event row count is recorded only as supporting context.
- The repair and mutant exist only in temporary copied builds. Their exact source diffs and applied hashes are retained as provenance.
- No business oracle, LLM detector, or numerical harm score was used.
