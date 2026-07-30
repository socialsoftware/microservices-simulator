# Quizzes — Domain Model

> Follows the structure defined in [`docs/templates/domain-model-template.md`](../../docs/templates/domain-model-template.md).

**How to use this file:**
1. Read §1–§2 to understand the entities, their attributes, and how they relate.
2. Read §3 to understand every consistency rule and its predicate.
3. See [`quizzes-full-2-aggregate-grouping.md`](quizzes-full-2-aggregate-grouping.md) for the concrete aggregate partitioning decision and its event-dependency consequences.

---

## §1 — Entities

Each entity lists only its own scalar attributes. Cross-entity references appear in §2. The **Owns** column lists value objects that live inside this entity's boundary and have no independent identity (they are created and deleted with the entity).

> **Soft-delete:** Every aggregate inherits `state: AggregateState` from the simulator `Aggregate` base class (values: `ACTIVE`, `INACTIVE`, `DELETED`). This field is **not** a domain attribute and must **not** appear in the entity table. It is set by `remove()` on the base class. Rules that predicate on deletion (e.g. `X.state == DELETED`) rely on this field.

| Entity | Attributes | Owns |
|---|---|---|
| **Course** | `name: String` (immutable), `type: CourseType (TECNICO \| EXTERNAL)` (immutable) | — |
| **User** | `name: String`, `username: String`, `role: Role (STUDENT \| TEACHER \| ADMIN)` (immutable), `active: Boolean` (default: false) | — |
| **Topic** | `name: String` | — |
| **Execution** | `acronym: String`, `academicTerm: String`, `endDate: LocalDateTime` | ExecutionStudent × N |
| **ExecutionStudent** | `userAggregateId: Integer`, `userName: String`, `userUsername: String`, `userVersion: Long`, `active: Boolean` | — |
| **Question** | `title: String`, `content: String`, `creationDate: LocalDateTime` | Option × N, QuestionTopic × N |
| **Option** | `sequence: Integer`, `optionKey: Integer`, `content: String`, `correct: Boolean` | — |
| **QuestionTopic** | `topicAggregateId: Integer`, `topicName: String`, `topicVersion: Long`, `courseAggregateId: Integer` | — |
| **Quiz** | `title: String`, `creationDate: LocalDateTime` (immutable), `availableDate: LocalDateTime`, `conclusionDate: LocalDateTime`, `resultsDate: LocalDateTime`, `quizType: QuizType (EXAM \| TEST \| GENERATED \| PROPOSED \| IN_CLASS \| EXTERNAL_QUIZ)`, `lastModifiedTime: LocalDateTime` (technical) | QuizQuestion × N |
| **QuizQuestion** | `questionAggregateId: Integer`, `questionVersion: Long`, `title: String`, `content: String` | — |
| **QuizAnswer** | `creationDate: LocalDateTime` (immutable), `answerDate: LocalDateTime` (immutable), `completed: Boolean` | QuestionAnswer × N |
| **QuestionAnswer** | `questionAggregateId: Integer`, `questionVersion: Long`, `correctOptionKey: Integer`, `optionSequenceChoice: Integer`, `optionKey: Integer`, `correct: Boolean`, `timeTaken: Integer` | — |
| **Tournament** | `startTime: LocalDateTime`, `endTime: LocalDateTime`, `numberOfQuestions: Integer`, `cancelled: Boolean`, `lastModifiedTime: LocalDateTime` (technical) | TournamentCreator × 1, TournamentParticipant × N, TournamentTopic × N |
| **TournamentCreator** | `userAggregateId: Integer`, `userName: String`, `userUsername: String`, `userVersion: Long` | — |
| **TournamentParticipant** | `userAggregateId: Integer`, `userName: String`, `userUsername: String`, `userVersion: Long`, `enrollTime: LocalDateTime` | TournamentParticipantQuizAnswer × 1 |
| **TournamentParticipantQuizAnswer** | `quizAnswerAggregateId: Integer`, `quizAnswerVersion: Long`, `answered: Boolean` (default: false), `numberOfAnswered: Integer` (default: 0), `numberOfCorrect: Integer` (default: 0), `firstAnswerTime: LocalDateTime` (default: null) | — |
| **TournamentTopic** | `topicAggregateId: Integer`, `topicName: String`, `topicVersion: Long`, `courseAggregateId: Integer` | — |

> **Single-reference snapshots:** where an aggregate holds exactly one reference to an external aggregate (e.g. `Quiz → Execution`, `QuizAnswer → Quiz`, `Tournament → Quiz`), the cached id/version pair is stored directly on the aggregate and is defined only in §2 of [`quizzes-full-2-aggregate-grouping.md`](quizzes-full-2-aggregate-grouping.md). The owned value objects above carry the cached fields for **collection** references, so that every rule in §3 that iterates over one has a grounded field list. Field names here and in grouping §2 are deliberately identical, so drift between the two files is visible.

> **Technical field note:** `lastModifiedTime` on `Quiz` and `Tournament` is not a domain attribute. It is stamped at mutation time so that `verifyInvariants()` can check temporal constraints (e.g., "fields are final after `availableDate`", "fields are final after `startTime`") without calling `now()` inside the invariant, which would make it non-deterministic across TCC merges. `firstAnswerTime` on `TournamentParticipantQuizAnswer` is a domain value carried by `QuizAnswerQuestionAnswerEvent`: it is the time the student actually answered, set once (idempotent) when the first answer for that participant arrives — never the time the event happened to be handled.

---

## §2 — Relationships

The direction is always from the referencing entity to the referenced entity. **Immutable** means the reference is set at creation and never changed.

| From | To | Cardinality | Immutable |
|---|---|---|---|
| Topic | Course | N → 1 | yes |
| Execution | Course | N → 1 | yes |
| Execution | User (students) | N → M | no (students enroll and are disenrolled) |
| Question | Course | N → 1 | yes |
| Question | Topic | N → M | no (topics can be updated) |
| Quiz | Execution | N → 1 | yes |
| Quiz | Question | N → M | no (frozen after `availableDate`) |
| QuizAnswer | Quiz | N → 1 | yes |
| QuizAnswer | User/student | N → 1 | yes |
| QuizAnswer | Execution | N → 1 | yes |
| QuestionAnswer | Question | N → 1 | yes |
| Tournament | Execution | N → 1 | yes |
| Tournament | User/creator | N → 1 | yes |
| Tournament | User/participants | N → M | no (participants enroll; one is removed when disenrolled from the execution, and the whole list is cleared when the tournament is deleted) |
| Tournament | Topic | N → M | no (frozen after `startTime`) |
| Tournament | Quiz | 1 → 1 | yes |
| Tournament | QuizAnswer (per participant) | N → M | no (updated as answers arrive) |

---

## §3 — Rules

### 3.1 — Single-entity rules

These rules inspect only fields of a single entity.

| Rule | Entity | Predicate |
|---|---|---|
| COURSE_TYPE_FINAL | Course | `Course.type` is immutable (Java `final` field) |
| COURSE_NAME_FINAL | Course | `Course.name` is immutable (Java `final` field) |
| USER_ROLE_FINAL | User | `User.role` is immutable (Java `final` field) |
| USER_DELETED_STATE | User | `User.state == DELETED ⟹ User.active == false` |
| QUIZ_CREATION_DATE_FINAL | Quiz | `Quiz.creationDate` is immutable (Java `final` field) |
| QUIZ_DATE_ORDERING | Quiz | `creationDate < availableDate < conclusionDate ≤ resultsDate` |
| QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE | Quiz | `Quiz.lastModifiedTime > prev.availableDate ⟹ availableDate, conclusionDate, resultsDate, questions are unchanged from prev` |
| TOURNAMENT_START_BEFORE_END_TIME | Tournament | `Tournament.startTime < Tournament.endTime` |
| TOURNAMENT_UNIQUE_AS_PARTICIPANT | Tournament | All entries in `Tournament.participants` have distinct `userAggregateId` |
| TOURNAMENT_ENROLL_UNTIL_START_TIME | Tournament | `∀p ∈ Tournament.participants: p.enrollTime < Tournament.startTime` |
| TOURNAMENT_FINAL_AFTER_START | Tournament | `Tournament.lastModifiedTime > prev.startTime ⟹ startTime, endTime, numberOfQuestions, topics, cancelled are unchanged from prev` |
| TOURNAMENT_IS_CANCELED | Tournament | `prev.cancelled == true ⟹ startTime, endTime, numberOfQuestions, topics, cancelled, participants are unchanged from prev` |
| TOURNAMENT_DELETE | Tournament | `Tournament.state == DELETED ⟹ Tournament.participants.isEmpty()` |
| TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY | Tournament | `∀p ∈ participants where p.userAggregateId == creator.userAggregateId: p.userName == creator.userName ∧ p.userUsername == creator.userUsername ∧ p.userVersion == creator.userVersion` |
| TOURNAMENT_ANSWER_BEFORE_START | Tournament | `∀p ∈ participants: p.quizAnswer.firstAnswerTime != null → p.quizAnswer.firstAnswerTime ≥ startTime` |

> **Immutability fields:** `TOURNAMENT_CREATOR_IS_FINAL`, `TOURNAMENT_COURSE_EXECUTION_IS_FINAL`, `TOURNAMENT_QUIZ_IS_FINAL`, `QUIZ_COURSE_EXECUTION_FINAL`, `QUIZANSWER_FINAL_USER`, `QUIZANSWER_FINAL_QUIZ`, `QUIZANSWER_FINAL_COURSE_EXECUTION`, `QUIZANSWER_FINAL_CREATION_DATE` are all enforced by Java `final` fields or by absence of setters after construction. No `verifyInvariants()` check is needed.

---

### 3.2 — Cross-entity rules

---

#### Rule: REMOVE_NO_STUDENTS

| Field | Value |
|---|---|
| Entities | Execution, User |
| Predicate | `Execution.state == DELETED ⟹ Execution.students.isEmpty()` |

---

#### Rule: NO_DUPLICATE_COURSE_EXECUTION

| Field | Value |
|---|---|
| Entities | Execution |
| Predicate | No two active Executions share the same `(acronym, academicTerm)` pair |

---

#### Rule: INACTIVE_USER

| Field | Value |
|---|---|
| Entities | Execution, User |
| Predicate | A User being enrolled in an Execution must have `active == true` |

---

#### Rule: STUDENT_ALREADY_ENROLLED

| Field | Value |
|---|---|
| Entities | Execution, User |
| Predicate | A User may appear in `Execution.students` at most once |

---

#### Rule: USER_EXISTS (Execution)

| Field | Value |
|---|---|
| Entities | Execution, User |
| Predicate | `∀s ∈ Execution.students: s references a User that has not been deleted` |

---

#### Rule: CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT *(deferred)*

> **Intentionally deferred.** This cross-aggregate rule — "a Course may not hold questions unless it
> has at least one execution" — was originally enforced via cached `executionCount` / `questionCount`
> counters on `Course`. That counter-based approach has been removed; the rule is to be re-implemented
> later by a different mechanism. No counters, count commands, or `Course.verifyInvariants()` logic
> currently enforce it.

---

#### Rule: TOPIC_BELONGS_TO_QUESTION_COURSE

| Field | Value |
|---|---|
| Entities | Question, Topic, Course |
| Predicate | `∀t ∈ Question.topics: t.courseAggregateId == Question.courseAggregateId` |

---

#### Rule: TOPICS_EXIST (Question)

| Field | Value |
|---|---|
| Entities | Question, Topic |
| Predicate | `∀t ∈ Question.topics: t references a Topic that has not been deleted` |

---

#### Rule: QUESTION_EXISTS (Quiz)

| Field | Value |
|---|---|
| Entities | Quiz, Question |
| Predicate | `∀q ∈ Quiz.questions: q references a Question that has not been deleted` |

---

#### Rule: COURSE_EXECUTION_EXISTS (Quiz)

| Field | Value |
|---|---|
| Entities | Quiz, Execution |
| Predicate | `Quiz.execution references an Execution that has not been deleted` |

---

#### Rule: UNIQUE_QUIZ_ANSWER_PER_STUDENT

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz, User |
| Predicate | At most one QuizAnswer may exist per `(quizAggregateId, userAggregateId)` pair |

---

#### Rule: QUESTION_ALREADY_ANSWERED

| Field | Value |
|---|---|
| Entities | QuizAnswer, Question |
| Predicate | `∀qa ∈ QuizAnswer.questionAnswers: all questionAggregateIds are distinct` |

---

#### Rule: ANSWER_MATCHES_CORRECT_OPTION

| Field | Value |
|---|---|
| Entities | QuizAnswer, Question, Option |
| Predicate | `∀qa ∈ QuizAnswer.questionAnswers where qa.optionKey != null: qa.correct == (qa.optionKey == qa.correctOptionKey)` — `correctOptionKey` is the `optionKey` of the Question's `correct` Option, cached on the QuestionAnswer when the QuizAnswer is created, so correctness is decided from local state at answer time |

---

#### Rule: COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz, Execution |
| Predicate | `QuizAnswer.executionAggregateId == QuizAnswer.quiz.executionAggregateId` |

---

#### Rule: USER_EXISTS (QuizAnswer)

| Field | Value |
|---|---|
| Entities | QuizAnswer, User, Execution |
| Predicate | `QuizAnswer.student references a User that has not been deleted or disenrolled` |

---

#### Rule: QUIZ_EXISTS (QuizAnswer)

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz, Question |
| Predicate | `QuizAnswer.quiz references a Quiz that has not been invalidated` — a Quiz is invalidated when any of its `questions` entries references a Question whose `state == DELETED`; once invalidated the Quiz must be treated as deleted for all downstream rules |

---

#### Rule: COURSE_EXECUTION_EXISTS (QuizAnswer)

| Field | Value |
|---|---|
| Entities | QuizAnswer, Execution |
| Predicate | `QuizAnswer.execution references an Execution that has not been deleted` |

---

#### Rule: CREATOR_IS_NOT_ANONYMOUS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, User |
| Predicate | `Tournament.creator.userName ≠ "ANONYMOUS" ∧ Tournament.creator.userUsername ≠ "ANONYMOUS"` |

---

#### Rule: CREATOR_COURSE_EXECUTION (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, User, Execution |
| Predicate | `Tournament.creator ∈ Tournament.execution.students` |

---

#### Rule: PARTICIPANT_COURSE_EXECUTION (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, User, Execution |
| Predicate | `∀p ∈ Tournament.participants: p ∈ Tournament.execution.students` — enrollment is checked when the participant is added, and the participant is removed from the Tournament when `DisenrollStudentFromCourseExecutionEvent` arrives for that student |

---

#### Rule: TOPIC_COURSE_EXECUTION (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Topic, Execution, Course |
| Predicate | `∀t ∈ Tournament.topics: t.courseAggregateId == Tournament.courseAggregateId` — the Tournament caches the Execution's `courseAggregateId` and each topic's `courseAggregateId`, so the check reads only local state |

---

#### Rule: QUIZ_COURSE_EXECUTION_CONSISTENCY (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Quiz, Execution |
| Predicate | `Tournament.quiz.executionAggregateId == Tournament.executionAggregateId` |

---

#### Rule: START_TIME_AVAILABLE_DATE / END_TIME_CONCLUSION_DATE (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Quiz |
| Predicate | `Tournament.startTime == Quiz.availableDate ∧ Tournament.endTime == Quiz.conclusionDate` |

---

#### Rule: NUMBER_OF_QUESTIONS / QUIZ_TOPICS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Quiz, Question, Topic |
| Predicate | `Tournament.numberOfQuestions == count(Quiz.questions) ∧ Tournament.topics ⊇ topics(Quiz.questions)` |

---

#### Rule: CREATOR_EXISTS / PARTICIPANT_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, User |
| Predicate | Creator and all participants reference Users that have not been deleted or anonymized |

---

#### Rule: TOPIC_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Topic |
| Predicate | `∀t ∈ Tournament.topics: t references a Topic that has not been deleted` |

---

#### Rule: QUIZ_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Quiz |
| Predicate | `Tournament.quiz references a Quiz that has not been invalidated` |

---

#### Rule: COURSE_EXECUTION_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Execution |
| Predicate | `Tournament.execution references an Execution that has not been deleted` |

---

#### Rule: QUIZ_ANSWER_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, QuizAnswer, QuestionAnswer |
| Predicate | `∀p ∈ participants: p.quizAnswer statistics reflect the actual QuizAnswer` (`answered`, `numberOfAnswered`, `numberOfCorrect`, `firstAnswerTime`) |

---

## §4 — Functionalities

> **Deviation from the template:** the template says operations touching a single aggregate should be omitted here. This file lists them anyway, so that §4 is a complete inventory of the application's operations. Single-aggregate rows carry an empty **Other Aggregates** column and need no saga coordination.
>
> **Other Aggregates** lists only aggregates the saga itself reads or writes. Aggregates that react asynchronously to a published event are not listed here — those relationships live in §3 and §4 of the aggregate grouping.

| Functionality | Primary Aggregate | Other Aggregates | Description |
|---|---|---|---|
| CreateCourse | Course | — | Create a new course. Courses are immutable and are never updated or deleted |
| CreateTopic | Topic | Course | Create a topic linked to a course |
| UpdateTopic | Topic | — | Update the name of an existing topic |
| DeleteTopic | Topic | — | Soft-delete a topic |
| CreateExecution | Execution | Course | Create a course execution linked to a course |
| UpdateExecution | Execution | — | Update execution acronym or academic term |
| DeleteExecution | Execution | — | Delete an execution |
| EnrollStudentInExecution | Execution | User | Enroll an active user in a course execution |
| DisenrollStudent | Execution | — | Remove a student from a course execution |
| CreateUser | User | — | Create a user account (inactive until activated) |
| ActivateUser | User | — | Activate a user account so it can be enrolled in executions |
| UpdateUserName | User | — | Update a user's name |
| AnonymizeUser | User | — | Anonymize a user (set name and username to ANONYMOUS) |
| DeleteUser | User | — | Soft-delete a user account |
| CreateQuestion | Question | Course, Topic | Create a question linked to a course and topics |
| UpdateQuestion | Question | Topic | Update question title, content or topics |
| DeleteQuestion | Question | — | Delete a question |
| CreateQuiz | Quiz | Execution, Question | Create a quiz linked to an execution and questions |
| UpdateQuiz | Quiz | — | Update quiz dates or questions (before available date) |
| CreateQuizAnswer | QuizAnswer | Quiz, User, Execution, Question | Start a student's answer session for a quiz, seeding one QuestionAnswer per quiz question with that question's `correctOptionKey` |
| AnswerQuestion | QuizAnswer | — | Record a student's answer to one question in a quiz |
| ConcludeQuiz | QuizAnswer | — | Mark a quiz answer session as completed |
| CreateTournament | Tournament | Execution, User, Topic, Quiz | Create a tournament for a course execution (also creates the associated Quiz) |
| AddParticipant | Tournament | Execution, User | Enroll a student as a tournament participant |
| UpdateTournament | Tournament | Quiz | Update tournament timing or topics |
| CancelTournament | Tournament | — | Cancel an open tournament |
| DeleteTournament | Tournament | — | Delete a cancelled or finished tournament, clearing its participant list in the same operation so that TOURNAMENT_DELETE holds |

---
