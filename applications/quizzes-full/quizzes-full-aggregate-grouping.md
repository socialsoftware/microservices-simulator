# Quizzes — Aggregate Grouping

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

This file captures the aggregate partitioning decision for the [Quizzes domain model](quizzes-full-domain-model.md).

All eight entities are placed in separate aggregates. This maximises deployment flexibility (each can be a separate microservice) but requires event-based eventual consistency for every cross-entity rule whose entities end up in different aggregates.

---

## §1 — Aggregate Grouping

| Aggregate | Description | Entities contained | Service |
|---|---|---|---|
| Course | A course offered by the institution (e.g. "Software Engineering"). Immutable after creation. Acts as the root namespace for Topics, Questions, and Executions. | Course | CourseService |
| Execution | A concrete run of a Course in a given academic term. Holds the enrolled student roster. Multiple executions can exist for the same Course. | Execution | ExecutionService |
| User | A person in the system — student, teacher, or admin. Tracks name, username, role, and whether the account is active. Role is immutable. | User | UserService |
| Topic | A subject tag that belongs to a Course. Used to classify Questions and to filter content for Tournaments. | Topic | TopicService |
| Question | A quiz question with its answer options. Tracks how many times it has been answered and how many answers were correct. Questions belong to a Course and are tagged with Topics. | Question, Option | QuestionService |
| Quiz | An ordered collection of Questions made available to students of an Execution between `availableDate` and `conclusionDate`. Fields are frozen once the quiz becomes available. | Quiz | QuizService |
| QuizAnswer | A student's response session for one Quiz. Records per-question answers (QuestionAnswer), completion status, and timing. At most one exists per student per quiz. | QuizAnswer, QuestionAnswer | QuizAnswerService |
| Tournament | A competitive event where students within an Execution race to answer a generated Quiz on selected Topics. Has an enrolment window (before `startTime`) and is frozen once started and answered. | Tournament | TournamentService |

---

## §2 — Snapshots

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| Topic | Course | `courseId` | n/a — Course fields are immutable |
| Execution | Course | `courseId`, `courseName`, `courseType` | n/a — Course fields are immutable |
| Execution | User × N (students) | `userId`, `userName`, `userUsername`, `active` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| Question | Course | `courseId` | n/a — Course fields are immutable |
| Question | Topic × N | `topicId`, `topicName`, `courseId` | `UpdateTopicEvent`, `DeleteTopicEvent` |
| Quiz | Execution | `executionId`, `executionVersion` | `DeleteCourseExecutionEvent` |
| Quiz | Question × N | `questionId`, `questionVersion`, `title`, `content` | `UpdateQuestionEvent`, `DeleteQuestionEvent` |
| QuizAnswer | Quiz | `quizId`, `quizVersion` | `InvalidateQuizEvent` |
| QuizAnswer | User/student | `userId`, `userName` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent` |
| QuizAnswer | Execution | `executionId`, `executionVersion` | `DeleteCourseExecutionEvent`, `DisenrollStudentFromCourseExecutionEvent` |
| QuestionAnswer | Question | `questionId`, `questionVersion` | `UpdateQuestionEvent` |
| Tournament | Execution | `executionId`, `executionVersion` | `DeleteCourseExecutionEvent` |
| Tournament | User/creator | `userId`, `userName`, `userUsername`, `userVersion` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| Tournament | User/participant × N | `userId`, `userName`, `userUsername`, `enrollTime`, `userVersion` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| Tournament | Topic × N | `topicId`, `topicName` | `UpdateTopicEvent`, `DeleteTopicEvent` |
| Tournament | Quiz | `quizId`, `quizVersion` | `InvalidateQuizEvent` |
| Tournament | QuizAnswer per participant | `quizAnswerId`, `quizAnswerVersion`, `answered`, `numberOfAnswered`, `numberOfCorrect`, `firstAnswerTime` | `QuizAnswerQuestionAnswerEvent` |

---

## §3 — Upstream / Downstream Event Dependencies

Because all entities are in separate aggregates, every cross-entity relationship requires event subscriptions for eventual-consistency rules. The arrows below list which aggregates are upstream publishers and which are downstream consumers.

```
Course ──────────────────────────► Topic
Course ──────────────────────────► Execution
Course ──────────────────────────► Question
Topic ───────────────────────────► Question
Topic ───────────────────────────► Tournament
User ────────────────────────────► Execution
User ────────────────────────────► QuizAnswer
User ────────────────────────────► Tournament
Question ────────────────────────► Quiz
Question ────────────────────────► QuizAnswer
Execution ───────────────────────► Quiz
Execution ───────────────────────► QuizAnswer
Execution ───────────────────────► Tournament
Quiz ────────────────────────────► QuizAnswer
Quiz ────────────────────────────► Tournament
QuizAnswer ──────────────────────► Tournament
```

> An arrow `A ──► B` means: B caches A's fields locally. If A's fields can change, B also subscribes to A's events (listed in §4) to keep the snapshot current. If A's fields are immutable (e.g. Course), no event subscription is needed — the snapshot is seeded once at B's creation time and never needs refreshing.

---

## §4 — Events

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| `DeleteUserEvent` | User | user soft-deleted | — | Execution, QuizAnswer, Tournament |
| `UpdateStudentNameEvent` | User | student name updated | `studentAggregateId`, `updatedName` | Execution, QuizAnswer, Tournament |
| `AnonymizeStudentEvent` | User | student anonymized | `studentAggregateId`, `name`, `username` | Execution, QuizAnswer, Tournament |
| `UpdateTopicEvent` | Topic | topic name changed | `topicName` | Question, Tournament |
| `DeleteTopicEvent` | Topic | topic soft-deleted | — | Question, Tournament |
| `UpdateQuestionEvent` | Question | question title/content changed | `title`, `content` | Quiz, QuestionAnswer |
| `DeleteQuestionEvent` | Question | question soft-deleted | `courseAggregateId` | Quiz |
| `DeleteCourseExecutionEvent` | Execution | execution soft-deleted | — | Quiz, QuizAnswer, Tournament |
| `DisenrollStudentFromCourseExecutionEvent` | Execution | student removed from execution | `studentAggregateId` | QuizAnswer |
| `InvalidateQuizEvent` | Quiz | a question in the quiz was soft-deleted | — | QuizAnswer, Tournament |
| `QuizAnswerQuestionAnswerEvent` | QuizAnswer | student answers a question | `questionAggregateId`, `quizAggregateId`, `studentAggregateId`, `correct` | Tournament |

---
