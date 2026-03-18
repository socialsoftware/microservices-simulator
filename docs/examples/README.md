# Examples

Worked examples of patterns implemented in the Quizzes application.

| Example | Demonstrates | File |
|---------|-------------|------|
| CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT | Inter-invariant: eventually-consistent counter maintained via events; guard enforced in service method | [cannot-delete-last-execution-with-content.md](cannot-delete-last-execution-with-content.md) |
| UNIQUE_QUIZ_ANSWER_PER_STUDENT | Inter-invariant: set-based deduplication cached in Quiz; guard checked in QuizAnswerService | [unique-quiz-answer-per-student.md](unique-quiz-answer-per-student.md) |
| Tournament inter-invariants | Inter-invariants: full set of cross-aggregate consistency rules for Tournament (creator/participant existence, course execution, topics, quiz, quiz answers) | [tournament-inter-invariants.md](tournament-inter-invariants.md) |
