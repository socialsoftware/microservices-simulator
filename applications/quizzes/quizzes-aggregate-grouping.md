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

| Aggregate | Snapshots of | Fields cached |
|---|---|---|
| Topic | Course | `courseId` |
| Execution | Course | `courseId`, `courseName`, `courseType` |
| Execution | User × N (students) | `userId`, `userName`, `userUsername`, `active` |
| Question | Course | `courseId` |
| Question | Topic × N | `topicId`, `topicName`, `courseId` |
| Quiz | Execution | `executionId`, `executionVersion` |
| Quiz | Question × N | `questionId`, `questionVersion`, `title`, `content` |
| QuizAnswer | Quiz | `quizId`, `quizVersion` |
| QuizAnswer | User/student | `userId`, `userName` |
| QuizAnswer | Execution | `executionId`, `executionVersion` |
| QuestionAnswer | Question | `questionId`, `questionVersion` |
| Tournament | Execution | `executionId`, `executionVersion` |
| Tournament | User/creator | `userId`, `userName`, `userUsername`, `userVersion` |
| Tournament | User/participant × N | `userId`, `userName`, `userUsername`, `enrollTime`, `userVersion` |
| Tournament | Topic × N | `topicId`, `topicName` |
| Tournament | Quiz | `quizId`, `quizVersion` |
| Tournament | QuizAnswer per participant | `quizAnswerId`, `quizAnswerVersion`, `answered`, `numberOfAnswered`, `numberOfCorrect` |

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
