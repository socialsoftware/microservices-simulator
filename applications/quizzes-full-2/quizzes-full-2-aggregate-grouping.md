# Quizzes — Aggregate Grouping

> Follows the structure defined in [`docs/templates/aggregate-grouping-template.md`](../../docs/templates/aggregate-grouping-template.md).

> **Provenance note — 2026-09-19.** This file and
> [`quizzes-full-2-domain-model.md`](quizzes-full-2-domain-model.md) were re-partitioned after the
> run that produced this application, so that the pair is one *plain domain* plus one *aggregate
> grouping* over it. Everything this file gained — the `Snapshot value objects` column, §2.b, the
> §3.a consistency policy, the §5 Rule realisation table and the snapshot-coherence obligations that
> used to be stated as domain rules — moved here from the domain model unchanged in substance.
> **Nothing the pair specifies changed**, and the application was not regenerated.

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

A few rules are neither co-located nor propagated: they are established once by the saga that
creates the aggregate and nothing can invalidate them afterwards. §5 records those as
`precondition`.

The per-rule record is the Rule realisation table in §5.

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

## §5 — Cross-file notes

### Rule realisation

One row per cross-entity rule in domain §3.2. `intra` means the rule resolves inside a single
aggregate of this grouping — either because the entities are co-located, or because the fields it
needs are carried by a snapshot in §2 that nothing can invalidate. `eventual` means an event in §4
repairs it after an upstream change. `precondition` means it is established when the operation runs
and nothing afterwards can violate it.

| Rule (domain §3.2) | Realisation | Note |
|---|---|---|
| REMOVE_NO_STUDENTS | intra | the roster is `ExecutionStudent` inside the Execution aggregate |
| NO_DUPLICATE_COURSE_EXECUTION | intra | own-table uniqueness inside the Execution aggregate |
| INACTIVE_USER | eventual | `active` on the `ExecutionStudent` snapshot, refreshed by `ActivateUserEvent` |
| STUDENT_ALREADY_ENROLLED | intra | over the local roster |
| USER_EXISTS (Execution) | eventual | `DeleteUserEvent` removes the student from the roster |
| CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT | — | deferred in the domain model; nothing realises it |
| TOPIC_BELONGS_TO_QUESTION_COURSE | intra | `QuestionTopic` caches `courseAggregateId`; a Topic's Course is immutable |
| TOPICS_EXIST (Question) | eventual | `DeleteTopicEvent` |
| QUESTION_EXISTS (Quiz) | eventual | `DeleteQuestionEvent`, which also makes the Quiz publish `InvalidateQuizEvent` |
| COURSE_EXECUTION_EXISTS (Quiz) | eventual | `DeleteCourseExecutionEvent` |
| UNIQUE_QUIZ_ANSWER_PER_STUDENT | intra | own-table uniqueness over the cached `(quizAggregateId, userAggregateId)` pair |
| QUESTION_ALREADY_ANSWERED | intra | `QuestionAnswer` is co-located with `QuizAnswer` |
| ANSWER_MATCHES_CORRECT_OPTION | intra | `correctOptionKey` is seeded onto each `QuestionAnswer` at creation; Options never change |
| COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION | intra | both execution ids are cached on QuizAnswer |
| USER_EXISTS (QuizAnswer) | eventual | `DeleteUserEvent` and `DisenrollStudentFromCourseExecutionEvent` |
| QUIZ_EXISTS (QuizAnswer) | eventual | `InvalidateQuizEvent` |
| COURSE_EXECUTION_EXISTS (QuizAnswer) | eventual | `DeleteCourseExecutionEvent` |
| CREATOR_IS_NOT_ANONYMOUS (Tournament) | eventual | `AnonymizeStudentEvent` refreshes the `TournamentCreator` snapshot |
| CREATOR_COURSE_EXECUTION (Tournament) | precondition | checked against the Execution the `CreateTournament` saga fetches; the creator reference is immutable |
| PARTICIPANT_COURSE_EXECUTION (Tournament) | eventual | `DisenrollStudentFromCourseExecutionEvent` drops the participant |
| TOPIC_COURSE_EXECUTION (Tournament) | intra | Tournament caches `courseAggregateId` on both its Execution snapshot and each `TournamentTopic` |
| QUIZ_COURSE_EXECUTION_CONSISTENCY (Tournament) | precondition | the `CreateTournament` saga creates the Quiz on the same Execution; both references are immutable |
| START_TIME_AVAILABLE_DATE / END_TIME_CONCLUSION_DATE (Tournament) | precondition | the saga sets both sides from one pair of values, and `UpdateTournament` updates both |
| NUMBER_OF_QUESTIONS / QUIZ_TOPICS (Tournament) | precondition | the saga generates the Quiz from the Tournament's topic set and question count |
| CREATOR_EXISTS / PARTICIPANT_EXISTS (Tournament) | eventual | `DeleteUserEvent`, `AnonymizeStudentEvent` |
| TOPIC_EXISTS (Tournament) | eventual | `DeleteTopicEvent` |
| QUIZ_EXISTS (Tournament) | eventual | `InvalidateQuizEvent` |
| COURSE_EXECUTION_EXISTS (Tournament) | eventual | `DeleteCourseExecutionEvent` |
| QUIZ_ANSWER_EXISTS (Tournament) | eventual | `QuizAnswerQuestionAnswerEvent` refreshes `TournamentParticipantQuizAnswer` |
| TOURNAMENT_UNIQUE_AS_PARTICIPANT | intra | `TournamentParticipant` is co-located with `Tournament` |
| TOURNAMENT_ENROLL_UNTIL_START_TIME | intra | co-located |
| TOURNAMENT_DELETE | intra | co-located; `DeleteTournament` clears the list in the same operation |
| TOURNAMENT_IS_CANCELED | intra | co-located; decided against `lastModifiedTime` (§2.b) |
| TOURNAMENT_ANSWER_BEFORE_START | intra | decided against `firstAnswerTime` on the local `TournamentParticipantQuizAnswer` |

> **This table is a policy record, not a classification.** Which pattern (P1–P4) each rule gets is
> derived from it by `/classify-and-plan`, not stated in it.

### Snapshot-coherence obligations

Obligations this grouping creates by copying data, which the domain model does not and cannot state.

- **`TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY`.** When the creator is also a participant, the
  `TournamentCreator` snapshot and that `TournamentParticipant`'s cached `User` fields are two copies
  of the same `User`, and they must agree:
  `∀p ∈ participants where p.userAggregateId == creator.userAggregateId: p.userName == creator.userName ∧ p.userUsername == creator.userUsername ∧ p.userVersion == creator.userVersion`.
  This was a domain rule until the 2026-09-19 re-partition. It is not one: it says nothing about the
  domain, only that this grouping's two caches of one entity must not drift. A grouping that stored
  the creator as a plain reference, or that kept one `User` snapshot shared by both roles, would not
  have it.
- **`correctOptionKey` seeding.** `ANSWER_MATCHES_CORRECT_OPTION` reads
  `QuestionAnswer.chosenOption.correct` in the domain. Here the `Option` lives in the Question
  aggregate, so the `CreateQuizAnswer` saga seeds each `QuestionAnswer` with the correct Option's key
  and the invariant compares `optionKey == correctOptionKey`. If Options ever became mutable, this
  row would need an event.
- **`TOPIC_COURSE_EXECUTION` is local only because of two cached fields.** It is a P1 intra-invariant
  only because Tournament caches `courseAggregateId` on both its Execution snapshot and each Topic
  snapshot. Dropping either field turns the rule into a cross-aggregate check.
- **Participant statistics are a cache.** `QUIZ_ANSWER_EXISTS` is stated in the domain as "the
  participant's statistics *are* the quantities over their QuizAnswer". Here they are a stored copy
  refreshed by `QuizAnswerQuestionAnswerEvent`, so the rule becomes a coherence obligation on that
  copy, satisfied within the event poll interval rather than instantaneously.

### Other notes

- **Course has no events.** Course fields are `final` and there is no `DeleteCourse` functionality, so the three Course snapshot rows in §2 are seeded once at creation and never refreshed. Adding course mutation or deletion later means adding an event *and* a `courseVersion` to those rows.
- **User publishes its own name changes.** `UpdateUserName` and `AnonymizeUser` are User-primary operations in §4 of the domain model. Execution, QuizAnswer and Tournament are consumers only — an aggregate must never subscribe to its own events.
- **Participants are removed, never "leave".** There is no standalone leave-tournament operation. A participant disappears either through `DisenrollStudentFromCourseExecutionEvent` or when `DeleteTournament` clears the list, which is what makes `TOURNAMENT_DELETE` reachable.
- **A no-cascade variant would be a second grouping file, not an edit to the domain.** Dropping the
  event subscriptions would turn every `eventual` row above into `precondition` and leave the domain
  model untouched — which is the property the plain-domain split exists to give.

---
