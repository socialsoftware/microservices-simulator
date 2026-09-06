# Nested participant setup bindings: final handoff

Status: complete and independently reviewed on 2026-09-05.

Earlier returned DTOs now survive inside participant constructors, assignments, lists,
sets and supported transforms. Setup stops before the exact measured target, including
targets inside fixture setup. No source occurrence or identity is guessed. The visitor
preserves source references inside nested recipes; the mapper reuses the existing value
language. Missing, conflicting, mistyped or omitted producers remain blocked.

## Measured improvement

| Ordinary single inputs | Before | After |
| --- | ---: | ---: |
| Accepted denominator | 796 | 796 |
| With source setup | 576 | 664 |
| Without setup | 1 | 1 |
| Static candidates | 577 | 665 |
| Blocked | 219 | 131 |

Exactly 88 input IDs gained candidacy, none lost: 85 CreateQuestion and three CreateQuiz.
Nested scalar property collections, including the deferred Tournament patterns, remain
outside the approved extension. The provisional 721 result included that broader pattern
and is superseded by this final scoped result.

Final package: `verifiers/target/astra-nested-bindings/final-generated/quizzes-20260905-125205-380/`.
Repeated package: `verifiers/target/astra-nested-bindings/repeat-generated/quizzes-20260905-125859-988/`.
All nine files are byte-identical, including after runtime qualification. Exact gained
and lost IDs, source context and counts are recorded under the same artifact root.

## Runtime proof

Four fresh Docker attempts executed ordinary persisted fault-free scenarios, with no
package editing or extra application authorization:

| Measured input source | Result | Exact persisted identity |
| --- | --- | --- |
| CreateQuiz / QuizAnswerEventHandlingTest | SUCCESS / EXACT | CourseExecution 2, Question 5, new Quiz 6 |
| CreateQuiz / StartQuizCompensationTest | SUCCESS / EXACT | CourseExecution 2, Question 6, new Quiz 7 |
| CreateQuiz / StartQuizTest | SUCCESS / EXACT | CourseExecution 2, Question 5, new Quiz 6 |
| CreateQuestion / StartQuizTest | SUCCESS / EXACT | Course 1, Topic 4, new Question 5 |

Every attempt proved successful setup, an empty event baseline, exact nested identities
and exactly one target creation. The question representative uses StartQuizTest's single
topic fixture instead of the plan's illustrative CancelTournamentTest, simplifying the
independent identity check without changing the coverage contract.

Run from the repository root:

```sh
docker compose run --rm --no-deps --pull never -T --entrypoint bash scenario-executor /reports/astra-nested-bindings/run-qualification.sh /reports/astra-nested-bindings/final-generated/quizzes-20260905-125205-380/scenario-catalog-manifest.json
```

The qualification-only Java observer, driver, selected scenario IDs, reports, identity
sidecars and logs are under `verifiers/target/astra-nested-bindings/`. It observes state
to verify this binding change; it does not implement an impact detector.

## Validation and limitations

Focused generic tests: 57 passed. Quizzes source integration: passed. Full current verifier
run: 734 tests in 47 suites, zero failures/errors/skips. Use `full-verifier-tests.log`, not
the mixed report-directory total: five obsolete XML reports add 53 stale tests. The earlier
778 directory total is therefore not a comparable current-suite baseline.

See HANDOFF.md for incremental source files and REVIEW.md for independent assessment.
Canonical current state, roadmap and ordered-setup decision were updated. Prior dirty
work was preserved. No commits, pushes or merges were performed. Runtime evidence covers
these four selected attempts, not all 665 static candidates. No impact behavior changed.
