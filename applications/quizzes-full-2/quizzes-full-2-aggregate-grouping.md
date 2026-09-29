# Quizzes — Aggregate Grouping

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

> **Provenance note — 2026-09-19.** This file and
> [`quizzes-full-2-domain-model.md`](quizzes-full-2-domain-model.md) were re-partitioned after the
> run that produced this application, so that the pair is one *plain domain* plus one *aggregate
> grouping* over it. Everything this file gained — the `Snapshot value objects` column, §2.b, the
> §3.a consistency policy, the §5 Rule realisation table and the snapshot-coherence obligations that
> used to be stated as domain rules — moved here from the domain model unchanged in substance.
> **Nothing the pair specifies changed**, and the application was not regenerated.
>
> **2026-09-27.** The §5 Rule realisation table was removed: `/classify-and-plan` now derives each
> rule's pattern from §1, §2, §3.a and §4, and no skill had read the table. The reasons it gave for
> the rules the cascade leaves unrepaired moved to §3.a. The last version with the table is in git
> at `50e31d84a`.
>
> **2026-09-29.** §5 Cross-file notes was removed: no skill read it, and the grouping template no
> longer has it. The last version with it is in git at `e56a384b1`.

This file captures **one** aggregate partitioning of the [Quizzes domain model](quizzes-full-2-domain-model.md).
Several such files may exist over that one domain; writing a second must require no edit to it.

The domain's eleven entities are placed in eight aggregates, one per independently deployable
concept. This maximises deployment flexibility (each can be a separate microservice) but requires
event-based eventual consistency for every cross-entity rule whose entities end up in different
aggregates.

Six of the domain's cross-entity relationships are realised as **snapshot value objects** —
local copies of another aggregate's data, listed in §1 and detailed in §2. They are classes this
grouping introduces; they are not domain entities, and a different grouping that co-located the two
sides would not have them at all.

---

## §1 — Aggregate Grouping

| Aggregate | Description | Entities contained | Snapshot value objects | Service |
|---|---|---|---|---|
| Course | A course offered by the institution (e.g. "Software Engineering"). Immutable after creation and never deleted. Acts as the root namespace for Topics, Questions, and Executions. | Course | — | CourseService |
| Execution | A concrete run of a Course in a given academic term. Holds the enrolled student roster. Multiple executions can exist for the same Course. | Execution | ExecutionStudent | ExecutionService |
| User | A person in the system — student, teacher, or admin. Tracks name, username, role, and whether the account is active. Role is immutable; an account starts inactive and must be activated before it can be enrolled. | User | — | UserService |
| Topic | A subject tag that belongs to a Course. Used to classify Questions and to filter content for Tournaments. | Topic | — | TopicService |
| Question | A quiz question with its answer options. Questions belong to a Course and are tagged with Topics. | Question, Option | QuestionTopic | QuestionService |
| Quiz | An ordered collection of Questions made available to students of an Execution between `availableDate` and `conclusionDate`. Fields are frozen once the quiz becomes available. | Quiz | QuizQuestion | QuizService |
| QuizAnswer | A student's response session for one Quiz. Records per-question answers (QuestionAnswer), completion status, and timing. At most one exists per student per quiz. | QuizAnswer, QuestionAnswer | — | QuizAnswerService |
| Tournament | A competitive event where students within an Execution race to answer a generated Quiz on selected Topics. Has an enrolment window (before `startTime`) and is frozen once started and answered. | Tournament, TournamentParticipant | TournamentCreator, TournamentTopic, TournamentParticipantQuizAnswer | TournamentService |

> **What the snapshot value objects stand for.** Each is a domain *relationship* realised as a
> stored copy, because this grouping puts the two ends in different aggregates:
>
> | Snapshot value object | Domain relationship it realises |
> |---|---|
> | `ExecutionStudent` | `Execution → User (students)` |
> | `QuestionTopic` | `Question → Topic` |
> | `QuizQuestion` | `Quiz → Question` |
> | `TournamentCreator` | `Tournament → User/creator` |
> | `TournamentTopic` | `Tournament → Topic` |
> | `TournamentParticipantQuizAnswer` | `TournamentParticipant → QuizAnswer` |
>
> `TournamentParticipant` is different: it is a **domain entity** (domain §1), co-located with
> `Tournament` here, and this grouping additionally hangs the `TournamentCreator`-shaped cached
> `User` fields and a `TournamentParticipantQuizAnswer` off it.

**Fields of each snapshot value object.** §2 says which source each copies from and which event
refreshes it; the declarations are here, since these classes exist only in this grouping and the
domain model cannot declare them.

| Snapshot value object | Fields |
|---|---|
| **ExecutionStudent** | `userAggregateId: Integer`, `userName: String`, `userUsername: String`, `userVersion: Long`, `active: Boolean` |
| **QuestionTopic** | `topicAggregateId: Integer`, `topicName: String`, `topicVersion: Long`, `courseAggregateId: Integer` |
| **QuizQuestion** | `questionAggregateId: Integer`, `questionVersion: Long`, `title: String`, `content: String` |
| **TournamentCreator** | `userAggregateId: Integer`, `userName: String`, `userUsername: String`, `userVersion: Long` |
| **TournamentTopic** | `topicAggregateId: Integer`, `topicName: String`, `topicVersion: Long`, `courseAggregateId: Integer` |
| **TournamentParticipantQuizAnswer** | `quizAnswerAggregateId: Integer`, `quizAnswerVersion: Long`, `answered: Boolean` (default: false), `numberOfAnswered: Integer` (default: 0), `numberOfCorrect: Integer` (default: 0), `firstAnswerTime: LocalDateTime` (default: null) |

The cached `User` fields this grouping adds to the `TournamentParticipant` entity are
`userAggregateId: Integer`, `userName: String`, `userUsername: String`, `userVersion: Long`,
alongside its domain attribute `enrollTime: LocalDateTime`. The cached `Question` fields it adds to
the `QuestionAnswer` entity are `questionAggregateId: Integer`, `questionVersion: Long`,
`correctOptionKey: Integer`.

---

## §2 — Snapshots

For each aggregate that references an entity in a different aggregate, the fields it caches locally.
The `*AggregateId` and `*Version` fields exist only in this realisation; the domain model names
neither.

> **Version fields:** every row whose "Updated on event" column names an event also caches the publisher's version alongside its id — `EventSubscription` is built from that pair. Rows marked `n/a` cache no version because they never subscribe.

| Aggregate | Snapshots of | Fields cached | Updated on event |
|---|---|---|---|
| Topic | Course | `courseAggregateId` | n/a — Course fields are immutable and Courses are never deleted |
| Execution | Course | `courseAggregateId`, `courseName`, `courseType` | n/a — Course fields are immutable and Courses are never deleted |
| Execution / ExecutionStudent × N | User (students) | `userAggregateId`, `userName`, `userUsername`, `userVersion`, `active` | `ActivateUserEvent`, `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| Question | Course | `courseAggregateId` | n/a — Course fields are immutable and Courses are never deleted |
| Question / QuestionTopic × N | Topic | `topicAggregateId`, `topicName`, `topicVersion`, `courseAggregateId` | `UpdateTopicEvent`, `DeleteTopicEvent` |
| Quiz | Execution | `executionAggregateId`, `executionVersion` | `DeleteCourseExecutionEvent` |
| Quiz / QuizQuestion × N | Question | `questionAggregateId`, `questionVersion`, `title`, `content` | `UpdateQuestionEvent`, `DeleteQuestionEvent` |
| QuizAnswer | Quiz | `quizAggregateId`, `quizVersion` | `InvalidateQuizEvent` |
| QuizAnswer | User/student | `userAggregateId`, `userName`, `userVersion` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| QuizAnswer | Execution | `executionAggregateId`, `executionVersion` | `DeleteCourseExecutionEvent`, `DisenrollStudentFromCourseExecutionEvent` |
| QuizAnswer / QuestionAnswer × N | Question | `questionAggregateId`, `questionVersion`, `correctOptionKey` | `UpdateQuestionEvent` |
| Tournament | Execution | `executionAggregateId`, `executionVersion`, `courseAggregateId` | `DeleteCourseExecutionEvent`, `DisenrollStudentFromCourseExecutionEvent` |
| Tournament / TournamentCreator | User/creator | `userAggregateId`, `userName`, `userUsername`, `userVersion` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| Tournament / TournamentParticipant × N | User | `userAggregateId`, `userName`, `userUsername`, `userVersion` | `UpdateStudentNameEvent`, `AnonymizeStudentEvent`, `DeleteUserEvent` |
| Tournament / TournamentTopic × N | Topic | `topicAggregateId`, `topicName`, `topicVersion`, `courseAggregateId` | `UpdateTopicEvent`, `DeleteTopicEvent` |
| Tournament | Quiz | `quizAggregateId`, `quizVersion` | `InvalidateQuizEvent` |
| Tournament / TournamentParticipantQuizAnswer × 1 per participant | QuizAnswer | `quizAnswerAggregateId`, `quizAnswerVersion`, `answered`, `numberOfAnswered`, `numberOfCorrect`, `firstAnswerTime` | `QuizAnswerQuestionAnswerEvent` |

> **`TournamentParticipant` carries `enrollTime` plus cached `User` fields.** `enrollTime` is the
> domain attribute of the participation (domain §1); the four `user*` fields on the same row are this
> grouping's snapshot of the `TournamentParticipant → User` relationship.

> **`answered`, `numberOfAnswered`, `numberOfCorrect` and `firstAnswerTime` are stored here, derived in the domain.** The domain model states them as quantities over the participant's `QuizAnswer` (domain §1, "A participant's answer statistics are derived, not stored"). This grouping realises them as cached fields on `TournamentParticipantQuizAnswer`, refreshed by `QuizAnswerQuestionAnswerEvent`. `firstAnswerTime` is the time the student actually answered, carried on the event and set once (idempotent) when the first answer for that participant arrives — never the time the event happened to be handled.

> **`correctOptionKey` on QuestionAnswer** is seeded by the `CreateQuizAnswer` saga from the Question's correct Option and never refreshed: `UpdateQuestion` changes title, content and topics, never the Options. The `UpdateQuestionEvent` subscription on that row exists to track `questionVersion`. This is how `ANSWER_MATCHES_CORRECT_OPTION` — a domain rule over `QuestionAnswer.chosenOption.correct` — is decided from local state at answer time.

> **Tournament's Execution snapshot** subscribes to `DisenrollStudentFromCourseExecutionEvent` so the handler can drop the matching participant, keeping `PARTICIPANT_COURSE_EXECUTION` true after a disenroll. The event's anchor is the Execution, which is why the subscription hangs off this row rather than off the participant row.

---

## §2.b — Technical fields

Fields that exist for implementation reasons rather than domain reasons.

| Aggregate | Field | Why |
|---|---|---|
| Quiz | `lastModifiedTime: LocalDateTime` | `QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE` says "once `availableDate` has passed these fields do not change". Stamping the mutation time lets `verifyInvariants()` decide that without calling `now()` inside the invariant, which would be non-deterministic across TCC merges |
| Tournament | `lastModifiedTime: LocalDateTime` | The same, for `TOURNAMENT_FINAL_AFTER_START` and `TOURNAMENT_IS_CANCELED` |

> **Soft-delete state.** The simulator's `Aggregate` base class provides `state: AggregateState`
> (`ACTIVE`, `INACTIVE`, `DELETED`) and sets it via `remove()`. It is not a domain attribute and
> appears in no entity row of the domain model. Wherever a domain rule says "`X` has been deleted",
> this realisation resolves it against that inherited field (`X.state == DELETED`).

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

### 3.a — Consistency policy: cascade, via events

This grouping **cascades**. Every arrow above whose upstream can change or be deleted carries an
event subscription (§4), and the downstream aggregate's handler repairs its snapshot when the event
arrives. The domain's §3.2 standing invariants are therefore genuinely maintained as standing
invariants — eventually, within the event poll interval — rather than merely checked once.

Concretely, this is what the cascade does for each family of domain rule:

- **Existence rules** (`USER_EXISTS`, `TOPICS_EXIST`, `QUESTION_EXISTS`, `TOPIC_EXISTS`,
  `COURSE_EXECUTION_EXISTS`, `CREATOR_EXISTS / PARTICIPANT_EXISTS`) are restored by a delete event:
  the downstream aggregate learns of the deletion and reacts, rather than holding a dangling
  reference.
- **Membership rules** (`PARTICIPANT_COURSE_EXECUTION`) are restored by
  `DisenrollStudentFromCourseExecutionEvent`, which drops the participant.
- **Derived-value rules** (`QUIZ_ANSWER_EXISTS`, `TOURNAMENT_ANSWER_BEFORE_START`) are maintained by
  `QuizAnswerQuestionAnswerEvent`, which refreshes the statistics.
- **Validity rules** (`QUIZ_EXISTS`) are maintained by `InvalidateQuizEvent`: "a Quiz has lost a
  question" is the domain condition, and `InvalidateQuizEvent` is the mechanism that tells the Quiz's
  consumers so.
- **Rules whose entities this grouping co-locates** — the five `TOURNAMENT_*` participant rules,
  `QUESTION_ALREADY_ANSWERED` — need no propagation at all and hold transactionally.

A few rules are neither co-located nor propagated. Each is **precondition**: established by the
saga that runs the operation, with nothing afterwards that can invalidate it, so no event is needed.
`/classify-and-plan` flags each `needs review - no repair event`, and these are the reasons the human
confirms the flags against:

- `CREATOR_COURSE_EXECUTION (Tournament)` — checked against the Execution the `CreateTournament`
  saga fetches; the creator reference is immutable.
- `QUIZ_COURSE_EXECUTION_CONSISTENCY (Tournament)` — the `CreateTournament` saga creates the Quiz on
  the same Execution, and both references are immutable.
- `START_TIME_AVAILABLE_DATE / END_TIME_CONCLUSION_DATE (Tournament)` — the saga sets both sides from
  one pair of values, and `UpdateTournament` updates both.
- `NUMBER_OF_QUESTIONS / QUIZ_TOPICS (Tournament)` — the saga generates the Quiz from the
  Tournament's topic set and question count.

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
