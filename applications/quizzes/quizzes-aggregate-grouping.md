# Quizzes — Aggregate Grouping

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

This file captures the aggregate partitioning decision for the [Quizzes domain model](quizzes-domain-model.md).

All eight entities are placed in separate aggregates. This maximises deployment flexibility (each can be a separate microservice) but requires event-based eventual consistency for every cross-entity rule whose entities end up in different aggregates.

---

## §1 — Aggregate Grouping

| Aggregate | Entities contained | Service |
|---|---|---|
| Course | Course | CourseService |
| Execution | Execution | ExecutionService |
| User | User | UserService |
| Topic | Topic | TopicService |
| Question | Question, Option | QuestionService |
| Quiz | Quiz | QuizService |
| QuizAnswer | QuizAnswer, QuestionAnswer | QuizAnswerService |
| Tournament | Tournament | TournamentService |

---

## §2 — Snapshots

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| Topic | Course | `courseId` | `CourseUpdatedEvent` |
| Execution | Course | `courseId`, `courseName`, `courseType` | `CourseUpdatedEvent` |
| Execution | User × N (students) | `userId`, `userName`, `userUsername`, `active` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `UserDeletedEvent` |
| Question | Course | `courseId` | `CourseUpdatedEvent` |
| Question | Topic × N | `topicId`, `topicName`, `courseId` | `TopicUpdatedEvent`, `TopicDeletedEvent` |
| Quiz | Execution | `executionId`, `executionVersion` | `ExecutionUpdatedEvent` |
| Quiz | Question × N | `questionId`, `questionVersion`, `title`, `content` | `QuestionUpdatedEvent`, `QuestionDeletedEvent` |
| QuizAnswer | Quiz | `quizId`, `quizVersion` | `QuizUpdatedEvent` |
| QuizAnswer | User/student | `userId`, `userName` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent` |
| QuizAnswer | Execution | `executionId`, `executionVersion` | `ExecutionUpdatedEvent` |
| QuestionAnswer | Question | `questionId`, `questionVersion` | `QuestionUpdatedEvent` |
| Tournament | Execution | `executionId`, `executionVersion` | `ExecutionUpdatedEvent` |
| Tournament | User/creator | `userId`, `userName`, `userUsername`, `userVersion` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent` |
| Tournament | User/participant × N | `userId`, `userName`, `userUsername`, `enrollTime`, `userVersion` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent` |
| Tournament | Topic × N | `topicId`, `topicName` | `TopicUpdatedEvent`, `TopicDeletedEvent` |
| Tournament | Quiz | `quizId`, `quizVersion` | `QuizUpdatedEvent` |
| Tournament | QuizAnswer per participant | `quizAnswerId`, `quizAnswerVersion`, `answered`, `numberOfAnswered`, `numberOfCorrect` | `QuizAnswerUpdatedEvent` |

---

## §3 — Upstream / Downstream Event Dependencies

Because all entities are in separate aggregates, every cross-entity relationship requires event subscriptions for eventual-consistency rules. The arrows below list which aggregates are upstream publishers and which are downstream consumers.

```
Course ──────────────────────────► Execution
Course ──────────────────────────► Question
Topic ───────────────────────────► Question
Topic ───────────────────────────► Tournament
User ────────────────────────────► Execution
User ────────────────────────────► QuizAnswer
User ────────────────────────────► Tournament
Question ────────────────────────► Quiz
Execution ───────────────────────► Quiz
Execution ───────────────────────► QuizAnswer
Execution ───────────────────────► Tournament
Quiz ────────────────────────────► Tournament
QuizAnswer ──────────────────────► Tournament
```

> An arrow `A ──► B` means: B must subscribe to A's events and cache the relevant A fields locally.

---

## §4 — Events

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| `CourseUpdatedEvent` | Course | course name/type changed | `courseId`, `courseName`, `courseType` | Execution, Question, Topic |
| `UserDeletedEvent` | User | user deleted | `userId` | Execution |
| `UpdateStudentNameEvent` | User | student name updated | `userId`, `userName`, `userUsername` | Execution, QuizAnswer, Tournament |
| `AnonymizeStudentEvent` | User | student anonymized | `userId`, `userName`, `userUsername` | Execution, QuizAnswer, Tournament |
| `TopicUpdatedEvent` | Topic | topic name changed | `topicId`, `topicName`, `courseId` | Question, Tournament |
| `TopicDeletedEvent` | Topic | topic deleted | `topicId` | Question, Tournament |
| `QuestionUpdatedEvent` | Question | question title/content changed | `questionId`, `questionVersion`, `title`, `content` | Quiz, QuestionAnswer |
| `QuestionDeletedEvent` | Question | question deleted | `questionId` | Quiz |
| `ExecutionUpdatedEvent` | Execution | execution updated or deleted | `executionId`, `executionVersion` | Quiz, QuizAnswer, Tournament |
| `QuizUpdatedEvent` | Quiz | quiz updated or invalidated | `quizId`, `quizVersion` | QuizAnswer, Tournament |
| `QuizAnswerUpdatedEvent` | QuizAnswer | quiz answer submitted or updated | `quizAnswerId`, `quizAnswerVersion`, `answered`, `numberOfAnswered`, `numberOfCorrect` | Tournament |

---
