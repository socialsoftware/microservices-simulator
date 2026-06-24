# Quizzes-Full — Aggregate Grouping

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

This file captures the aggregate partitioning decision for the [Quizzes-Full domain model](quizzes-full-domain-model.md).

All entities are placed in separate aggregates, each corresponding to one microservice. This maximises deployment flexibility and follows the same philosophy as the simplified [quizzes application](../quizzes/quizzes-aggregate-grouping.md), extended with four additional aggregates (Assessment, Discussion, QuestionSubmission, Dashboard) to cover the full quizzes-tutor feature set.

Small value objects with no independent identity are co-located with their owning aggregate:
- `Option`, `Image` live inside **Question**
- `QuizQuestion` lives inside **Quiz**
- `QuestionAnswer` lives inside **QuizAnswer**
- `TopicConjunction` lives inside **Assessment**
- `Reply` lives inside **Discussion**
- `Review` lives inside **QuestionSubmission**
- `WeeklyScore`, `FailedAnswer`, `DifficultQuestion` live inside **Dashboard**

---

## §1 — Aggregate Grouping

| Aggregate | Description | Entities contained | Service |
|---|---|---|---|
| Course | A course offered by the institution (e.g. "Software Engineering"). Immutable after creation. Acts as the root namespace for Topics, Questions, and CourseExecutions. | Course | CourseService |
| User | A person in the system — student, teacher, or admin. Tracks name, username, role, and whether the account is active. Role is immutable. | User | UserService |
| Topic | A subject tag that belongs to a Course. Used to classify Questions and to filter content for Tournaments and Assessments. | Topic | TopicService |
| CourseExecution | A concrete run of a Course in a given academic term (e.g. "ES 2024/25 Leic"). Holds the enrolled student roster. Multiple executions can exist for the same Course. | CourseExecution | CourseExecutionService |
| Question | A quiz question with its answer options (and optional image). Tracks how many times it has been answered and how many answers were correct. Questions belong to a Course and are tagged with Topics. | Question, Option, Image | QuestionService |
| Quiz | An ordered collection of Questions made available to students of a CourseExecution between `availableDate` and `conclusionDate`. Fields are frozen once the quiz becomes available. | Quiz, QuizQuestion | QuizService |
| QuizAnswer | A student's response session for one Quiz. Records per-question answers (QuestionAnswer), completion status, and timing. At most one exists per student per quiz. | QuizAnswer, QuestionAnswer | QuizAnswerService |
| Tournament | A competitive event where students within a CourseExecution race to answer a generated Quiz on selected Topics. Has an enrolment window (before `startTime`) and is frozen once started and answered. | Tournament | TournamentService |
| Assessment | A named set of topic groups (TopicConjunctions) that a teacher defines for a CourseExecution. Used to scope question searches and quiz generation — it is a discovery/filter aid, not a quiz itself. | Assessment, TopicConjunction | AssessmentService |
| Discussion | A threaded conversation started by a student on a specific QuestionAnswer. Teachers and the original student can add Replies. Discussions can be closed once at least one Reply exists. | Discussion, Reply | DiscussionService |
| QuestionSubmission | A student-proposed Question sent for teacher review within a CourseExecution. Carries a review workflow (IN_REVIEW → APPROVED / REJECTED) with Review comments attached. | QuestionSubmission, Review | QuestionSubmissionService |
| Dashboard | A per-student, per-CourseExecution analytics board. Tracks quiz and answer counts by quiz type, failed answers, statistically difficult questions, and weekly performance snapshots. Updated reactively from QuizAnswer events. | Dashboard, WeeklyScore, FailedAnswer, DifficultQuestion | DashboardService |

---

## §2 — Snapshots

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| Topic | Course | `courseId` | `CourseUpdatedEvent` |
| CourseExecution | Course | `courseId`, `courseName`, `courseType` | `CourseUpdatedEvent` |
| CourseExecution | User × N (students) | `userId`, `userName`, `userUsername`, `active` | `UserUpdatedEvent`, `UserAnonymizedEvent`, `UserDeletedEvent` |
| Question | Course | `courseId` | `CourseUpdatedEvent` |
| Question | Topic × N | `topicId`, `topicName`, `courseId` | `TopicUpdatedEvent`, `TopicDeletedEvent` |
| Quiz | CourseExecution | `executionId`, `executionVersion` | `CourseExecutionUpdatedEvent` |
| Quiz | Question × N (via QuizQuestion) | `questionId`, `questionVersion`, `title`, `content` | `QuestionUpdatedEvent`, `QuestionDeletedEvent` |
| QuizAnswer | Quiz | `quizId`, `quizVersion`, `oneWay`, `quizType` | `QuizUpdatedEvent` |
| QuizAnswer | User/student | `userId`, `userName` | `UserUpdatedEvent`, `UserAnonymizedEvent` |
| QuestionAnswer | Question (via QuizQuestion) | `questionId`, `questionVersion` | `QuestionUpdatedEvent` |
| Tournament | CourseExecution | `executionId`, `executionVersion` | `CourseExecutionUpdatedEvent` |
| Tournament | User/creator | `userId`, `userName`, `userUsername`, `userVersion` | `UserUpdatedEvent`, `UserAnonymizedEvent` |
| Tournament | User/participant × N | `userId`, `userName`, `userUsername`, `enrollTime`, `userVersion` | `UserUpdatedEvent`, `UserAnonymizedEvent` |
| Tournament | Topic × N | `topicId`, `topicName` | `TopicUpdatedEvent`, `TopicDeletedEvent` |
| Tournament | Quiz | `quizId`, `quizVersion` | `QuizUpdatedEvent` |
| Tournament | QuizAnswer per participant | `quizAnswerId`, `quizAnswerVersion`, `answered`, `numberOfAnswered`, `numberOfCorrect` | `QuizAnswerUpdatedEvent` |
| Assessment | CourseExecution | `executionId`, `executionVersion`, `courseId` | `CourseExecutionUpdatedEvent` |
| Assessment | Topic × N (via TopicConjunction) | `topicId`, `topicName`, `courseId` | `TopicUpdatedEvent`, `TopicDeletedEvent` |
| Discussion | CourseExecution | `executionId`, `executionVersion` | `CourseExecutionUpdatedEvent` |
| Discussion | User/student | `userId`, `userName`, `userRole` | `UserUpdatedEvent`, `UserAnonymizedEvent` |
| Discussion | Question | `questionId`, `questionVersion` | `QuestionUpdatedEvent` |
| Discussion | QuestionAnswer | `questionAnswerId`, `questionAnswerVersion` | `QuizAnswerUpdatedEvent` |
| Reply (within Discussion) | User | `userId`, `userName`, `userRole` | `UserUpdatedEvent`, `UserAnonymizedEvent` |
| QuestionSubmission | Question | `questionId`, `questionVersion`, `title` | `QuestionUpdatedEvent` |
| QuestionSubmission | User/student | `userId`, `userName` | `UserUpdatedEvent`, `UserAnonymizedEvent` |
| QuestionSubmission | CourseExecution | `executionId`, `executionVersion`, `courseId` | `CourseExecutionUpdatedEvent` |
| Review (within QuestionSubmission) | User | `userId`, `userName`, `userRole` | `UserUpdatedEvent`, `UserAnonymizedEvent` |
| Dashboard | User/student | `userId` | — (set at creation, immutable) |
| Dashboard | CourseExecution | `executionId`, `executionVersion`, `courseId` | `CourseExecutionUpdatedEvent` |
| Dashboard | DifficultQuestion (internal) | `questionId`, `questionVersion`, `percentage`, `courseExecutionId` | `QuizAnswerUpdatedEvent` |
| Dashboard | FailedAnswer (internal) | `questionAnswerId`, `questionAnswerVersion`, `correct`, `completed` | `QuizAnswerUpdatedEvent` |

---

## §3 — Upstream / Downstream Event Dependencies

Because all entities are in separate aggregates, every cross-entity relationship requires event subscriptions for eventual-consistency rules. The arrows below list which aggregates are upstream publishers and which are downstream consumers.

```
Course ──────────────────────────► CourseExecution
Course ──────────────────────────► Question
Topic ───────────────────────────► Question
Topic ───────────────────────────► Assessment
Topic ───────────────────────────► Tournament
User ────────────────────────────► CourseExecution
User ────────────────────────────► QuizAnswer
User ────────────────────────────► Tournament
User ────────────────────────────► Discussion
User ────────────────────────────► Reply
User ────────────────────────────► QuestionSubmission
User ────────────────────────────► Review
Question ────────────────────────► Quiz
Question ────────────────────────► QuestionSubmission
Question ────────────────────────► Discussion
Question ────────────────────────► Dashboard
CourseExecution ─────────────────► Quiz
CourseExecution ─────────────────► Tournament
CourseExecution ─────────────────► Assessment
CourseExecution ─────────────────► Discussion
CourseExecution ─────────────────► QuestionSubmission
CourseExecution ─────────────────► Dashboard
Quiz ────────────────────────────► Tournament
QuizAnswer ──────────────────────► Tournament
QuizAnswer ──────────────────────► Dashboard
QuestionAnswer ──────────────────► Discussion
QuestionAnswer ──────────────────► Dashboard
```

> An arrow `A ──► B` means: B must subscribe to A's events and cache the relevant A fields locally.

---

## §4 — Events

Name every domain event published by each upstream aggregate. There must be one row per event — if two consumers subscribe to the same event, they share a single row here.

> **Trigger** — the service operation (or state change) that causes the event to be published.
> **Payload** — the fields the event carries (derived from §2 snapshot fields of the consumer(s)).
> **Consumer(s)** — which aggregates subscribe to this event (must match arrows in §3).

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| `CourseUpdatedEvent` | Course | course name or type changed | `courseId`, `courseName`, `courseType` | CourseExecution, Question, Topic |
| `UserDeletedEvent` | User | user deleted | `userId` | CourseExecution |
| `UserUpdatedEvent` | User | student name or username updated | `userId`, `userName`, `userUsername` | CourseExecution, QuizAnswer, Tournament, Discussion, QuestionSubmission |
| `UserAnonymizedEvent` | User | student anonymized | `userId`, `userName`, `userUsername` | CourseExecution, QuizAnswer, Tournament, Discussion, QuestionSubmission |
| `TopicUpdatedEvent` | Topic | topic name changed | `topicId`, `topicName`, `courseId` | Question, Assessment, Tournament |
| `TopicDeletedEvent` | Topic | topic deleted | `topicId` | Question, Assessment, Tournament |
| `QuestionUpdatedEvent` | Question | question title or content changed | `questionId`, `questionVersion`, `title`, `content` | Quiz, QuestionAnswer, Discussion, QuestionSubmission, Dashboard |
| `QuestionDeletedEvent` | Question | question deleted | `questionId` | Quiz, Dashboard |
| `CourseExecutionUpdatedEvent` | CourseExecution | execution updated or deleted | `executionId`, `executionVersion` | Quiz, Tournament, Assessment, Discussion, QuestionSubmission, Dashboard |
| `QuizUpdatedEvent` | Quiz | quiz updated or invalidated | `quizId`, `quizVersion` | QuizAnswer, Tournament |
| `QuizAnswerUpdatedEvent` | QuizAnswer | quiz answer submitted or updated | `quizAnswerId`, `quizAnswerVersion`, `answered`, `numberOfAnswered`, `numberOfCorrect` | Tournament, Dashboard, Discussion |

---
