# Quizzes — Aggregate Grouping

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

This file captures the aggregate partitioning decision for the [Quizzes domain model](quizzes-full-2-domain-model.md).

All eight entities are placed in separate aggregates. This maximises deployment flexibility (each can be a separate microservice) but requires event-based eventual consistency for every cross-entity rule whose entities end up in different aggregates.

---

## §1 — Aggregate Grouping

| Aggregate | Description | Entities contained | Service |
|---|---|---|---|
| Course | A course offered by the institution (e.g. "Software Engineering"). Immutable after creation and never deleted. Acts as the root namespace for Topics, Questions, and Executions. | Course | CourseService |
| Execution | A concrete run of a Course in a given academic term. Holds the enrolled student roster. Multiple executions can exist for the same Course. | Execution, ExecutionStudent | ExecutionService |
| User | A person in the system — student, teacher, or admin. Tracks name, username, role, and whether the account is active. Role is immutable; an account starts inactive and must be activated before it can be enrolled. | User | UserService |
| Topic | A subject tag that belongs to a Course. Used to classify Questions and to filter content for Tournaments. | Topic | TopicService |
| Question | A quiz question with its answer options. Questions belong to a Course and are tagged with Topics. | Question, Option, QuestionTopic | QuestionService |
| Quiz | An ordered collection of Questions made available to students of an Execution between `availableDate` and `conclusionDate`. Fields are frozen once the quiz becomes available. | Quiz, QuizQuestion | QuizService |
| QuizAnswer | A student's response session for one Quiz. Records per-question answers (QuestionAnswer), completion status, and timing. At most one exists per student per quiz. | QuizAnswer, QuestionAnswer | QuizAnswerService |
| Tournament | A competitive event where students within an Execution race to answer a generated Quiz on selected Topics. Has an enrolment window (before `startTime`) and is frozen once started and answered. | Tournament, TournamentCreator, TournamentParticipant, TournamentParticipantQuizAnswer, TournamentTopic | TournamentService |

---

## §2 — Snapshots

Field names below match the owned value objects declared in §1 of the [domain model](quizzes-full-2-domain-model.md).

> **Version fields:** every row whose "Updated on event" column names an event also caches the publisher's version alongside its id — `EventSubscription` is built from that pair. Rows marked `n/a` cache no version because they never subscribe.

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| Topic | Course | `courseAggregateId` | n/a — Course fields are immutable and Courses are never deleted |
| Execution | Course | `courseAggregateId`, `courseName`, `courseType` | n/a — Course fields are immutable and Courses are never deleted |
| Execution | User × N (students) | `userAggregateId`, `userName`, `userUsername`, `userVersion`, `active` | `ActivateUserEvent`, `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| Question | Course | `courseAggregateId` | n/a — Course fields are immutable and Courses are never deleted |
| Question | Topic × N | `topicAggregateId`, `topicName`, `topicVersion`, `courseAggregateId` | `UpdateTopicEvent`, `DeleteTopicEvent` |
| Quiz | Execution | `executionAggregateId`, `executionVersion` | `DeleteCourseExecutionEvent` |
| Quiz | Question × N | `questionAggregateId`, `questionVersion`, `title`, `content` | `UpdateQuestionEvent`, `DeleteQuestionEvent` |
| QuizAnswer | Quiz | `quizAggregateId`, `quizVersion` | `InvalidateQuizEvent` |
| QuizAnswer | User/student | `userAggregateId`, `userName`, `userVersion` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| QuizAnswer | Execution | `executionAggregateId`, `executionVersion` | `DeleteCourseExecutionEvent`, `DisenrollStudentFromCourseExecutionEvent` |
| QuizAnswer / QuestionAnswer × N | Question | `questionAggregateId`, `questionVersion`, `correctOptionKey` | `UpdateQuestionEvent` |
| Tournament | Execution | `executionAggregateId`, `executionVersion`, `courseAggregateId` | `DeleteCourseExecutionEvent`, `DisenrollStudentFromCourseExecutionEvent` |
| Tournament | User/creator | `userAggregateId`, `userName`, `userUsername`, `userVersion` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| Tournament | User/participant × N | `userAggregateId`, `userName`, `userUsername`, `userVersion`, `enrollTime` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| Tournament | Topic × N | `topicAggregateId`, `topicName`, `topicVersion`, `courseAggregateId` | `UpdateTopicEvent`, `DeleteTopicEvent` |
| Tournament | Quiz | `quizAggregateId`, `quizVersion` | `InvalidateQuizEvent` |
| Tournament | QuizAnswer per participant | `quizAnswerAggregateId`, `quizAnswerVersion`, `answered`, `numberOfAnswered`, `numberOfCorrect`, `firstAnswerTime` | `QuizAnswerQuestionAnswerEvent` |

> **`correctOptionKey` on QuestionAnswer** is seeded by the `CreateQuizAnswer` saga and never refreshed: `UpdateQuestion` changes title, content and topics, never the Options. The `UpdateQuestionEvent` subscription on that row exists to track `questionVersion`.

> **Tournament's Execution snapshot** subscribes to `DisenrollStudentFromCourseExecutionEvent` so the handler can drop the matching participant, keeping `PARTICIPANT_COURSE_EXECUTION` true after a disenroll. The event's anchor is the Execution, which is why the subscription hangs off this row rather than off the participant row.

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

> An arrow `A ──► B` means: B caches A's fields locally. If A's fields can change, B also subscribes to A's events (listed in §4) to keep the snapshot current. If A's fields are immutable and A is never deleted (Course), no event subscription is needed — the snapshot is seeded once at B's creation time and never needs refreshing.

---

## §4 — Events

| Event | Publisher | Trigger | Payload fields | Consumer(s) |
|---|---|---|---|---|
| `ActivateUserEvent` | User | user account activated | `userAggregateId` (anchor), `active` | Execution |
| `UpdateStudentNameEvent` | User | user name updated | `studentAggregateId` (anchor), `updatedName` | Execution, QuizAnswer, Tournament |
| `AnonymizeStudentEvent` | User | user anonymized | `studentAggregateId` (anchor), `name`, `username` | Execution, QuizAnswer, Tournament |
| `DeleteUserEvent` | User | user soft-deleted | `userAggregateId` (anchor) | Execution, QuizAnswer, Tournament |
| `UpdateTopicEvent` | Topic | topic name changed | `topicAggregateId` (anchor), `topicName` | Question, Tournament |
| `DeleteTopicEvent` | Topic | topic soft-deleted | `topicAggregateId` (anchor) | Question, Tournament |
| `UpdateQuestionEvent` | Question | question title/content changed | `questionAggregateId` (anchor), `title`, `content` | Quiz, QuizAnswer |
| `DeleteQuestionEvent` | Question | question soft-deleted | `questionAggregateId` (anchor), `courseAggregateId` | Quiz |
| `DeleteCourseExecutionEvent` | Execution | execution soft-deleted | `executionAggregateId` (anchor) | Quiz, QuizAnswer, Tournament |
| `DisenrollStudentFromCourseExecutionEvent` | Execution | student removed from execution | `executionAggregateId` (anchor), `studentAggregateId` | QuizAnswer, Tournament |
| `InvalidateQuizEvent` | Quiz | a question in the quiz was soft-deleted | `quizAggregateId` (anchor) | QuizAnswer, Tournament |
| `QuizAnswerQuestionAnswerEvent` | QuizAnswer | student answers a question | `quizAnswerAggregateId` (anchor), `questionAggregateId`, `quizAggregateId`, `studentAggregateId`, `correct`, `answerTime` | Tournament |

> **Anchor field:** the field marked `(anchor)` is the publisher aggregate's own ID. It is passed to `super(anchorAggregateId)` in the event constructor and must match the `subscribedAggregateId` used in the corresponding `EventSubscription` subclass. Without this, event filtering is broken. See [`docs/concepts/events.md`](../../docs/concepts/events.md) canonical wiring for the exact pattern.

> **`QuizAnswerQuestionAnswerEvent` payload:** `quizAnswerAggregateId` and `answerTime` let the Tournament link the participant's QuizAnswer on the first answer and store the authoritative `firstAnswerTime`, so `TOURNAMENT_ANSWER_BEFORE_START` does not depend on the ~1 s event poll lag. `quizAggregateId` and `studentAggregateId` identify which participant the statistics belong to.

---

## §5 — Cross-file consistency notes

Points where these two files constrain each other, recorded so they get re-checked when either changes:

- **Course has no events.** Course fields are `final` and there is no `DeleteCourse` functionality, so the three Course snapshot rows in §2 are seeded once at creation and never refreshed. Adding course mutation or deletion later means adding an event *and* a `courseVersion` to those rows.
- **User publishes its own name changes.** `UpdateUserName` and `AnonymizeUser` are User-primary operations in §4 of the domain model. Execution, QuizAnswer and Tournament are consumers only — an aggregate must never subscribe to its own events.
- **Participants are removed, never "leave".** There is no standalone leave-tournament operation. A participant disappears either through `DisenrollStudentFromCourseExecutionEvent` or when `DeleteTournament` clears the list, which is what makes `TOURNAMENT_DELETE` reachable.
- **`TOPIC_COURSE_EXECUTION` is local.** It is a P1 intra-invariant only because Tournament caches `courseAggregateId` on both its Execution snapshot and each Topic snapshot. Dropping either field turns the rule into a cross-aggregate check.

---
