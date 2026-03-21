# Examples

Worked examples of patterns implemented in the Quizzes application.

| Example | Demonstrates | File |
|---------|-------------|------|
| CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT | Functionality-level guard: `courseQuestionCount` and `courseExecutionCount` cached on `Course` aggregate (strongly consistent via saga steps); invariant checked in a named `getCourseStep` during `RemoveCourseExecution` | [cannot-delete-last-execution-with-content.md](cannot-delete-last-execution-with-content.md) |
| Tournament inter-invariants | Inter-invariants: full set of cross-aggregate consistency rules for Tournament (creator/participant existence, course execution, topics, quiz, quiz answers) | [tournament-inter-invariants.md](tournament-inter-invariants.md) |
