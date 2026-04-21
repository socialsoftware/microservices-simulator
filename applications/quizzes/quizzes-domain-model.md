# Quizzes — Domain Model

> Follows the structure defined in [`docs/templates/domain-model-template.md`](../../docs/templates/domain-model-template.md).

**How to use this file:**
1. Read §1–§2 to understand the entities, their attributes, and how they relate.
2. Read §3 to understand every consistency rule and its predicate.
3. See [`quizzes-aggregate-grouping.md`](quizzes-aggregate-grouping.md) for the concrete aggregate partitioning decision and its event-dependency consequences.

---

## §1 — Entities

Each entity lists only its own scalar attributes. Cross-entity references appear in §2. The **Owns** column lists value objects that live inside this entity's boundary and have no independent identity (they are created and deleted with the entity).

> **Soft-delete:** Every aggregate inherits `state: AggregateState` from the simulator `Aggregate` base class (values: `ACTIVE`, `INACTIVE`, `DELETED`). This field is **not** a domain attribute and must **not** appear in the entity table. It is set by `remove()` on the base class. Rules that predicate on deletion (e.g. `X.state == DELETED`) rely on this field.

| Entity | Attributes | Owns |
|---|---|---|
| **Course** | `name: String` (immutable), `type: CourseType (TECNICO \| EXTERNAL)` (immutable), `executionCount: Integer` (cached counter), `questionCount: Integer` (cached counter) | — |
| **User** | `name: String`, `username: String`, `role: Role (STUDENT \| TEACHER \| ADMIN)` (immutable), `active: Boolean` (default: false) | — |
| **Topic** | `name: String` | — |
| **Execution** | `acronym: String`, `academicTerm: String`, `endDate: LocalDateTime` | — |
| **Question** | `title: String`, `content: String`, `creationDate: LocalDateTime` | Option × N |
| **Option** | `sequence: Integer`, `optionKey: Integer`, `content: String`, `correct: Boolean` | — |
| **Quiz** | `title: String`, `creationDate: LocalDateTime` (immutable), `availableDate: LocalDateTime`, `conclusionDate: LocalDateTime`, `resultsDate: LocalDateTime`, `quizType: QuizType (EXAM \| TEST \| GENERATED \| PROPOSED \| IN_CLASS \| EXTERNAL_QUIZ)`, `lastModifiedTime: LocalDateTime` (technical) | — |
| **QuizAnswer** | `creationDate: LocalDateTime` (immutable), `answerDate: LocalDateTime` (immutable), `completed: Boolean` | QuestionAnswer × N |
| **QuestionAnswer** | `optionSequenceChoice: Integer`, `optionKey: Integer`, `correct: Boolean`, `timeTaken: Integer` | — |
| **Tournament** | `startTime: LocalDateTime`, `endTime: LocalDateTime`, `numberOfQuestions: Integer`, `cancelled: Boolean`, `lastModifiedTime: LocalDateTime` (technical) | `TournamentParticipant × N` |
| **TournamentParticipant** | `participantAggregateId: Integer`, `participantName: String`, `participantUsername: String`, `participantVersion: Long`, `enrollTime: LocalDateTime` | `TournamentParticipantQuizAnswer × 1` |
| **TournamentParticipantQuizAnswer** | `quizAnswerAggregateId: Integer`, `quizAnswerVersion: Long`, `answered: Boolean` (default: false), `numberOfAnswered: Integer` (default: 0), `numberOfCorrect: Integer` (default: 0) | — |

> **Technical field note:** `lastModifiedTime` on `Quiz` and `Tournament` is not a domain attribute. It is stamped at mutation time by each setter so that `verifyInvariants()` can check temporal constraints (e.g., "fields are final after `availableDate`") without calling `now()` inside the invariant check, which would make it non-idempotent across TCC merges.

---

## §2 — Relationships

The direction is always from the referencing entity to the referenced entity. **Immutable** means the reference is set at creation and never changed.

| From | To | Cardinality | Immutable |
|---|---|---|---|
| Topic | Course | N → 1 | yes |
| Execution | Course | N → 1 | yes |
| Execution | User (students) | N → M | no (students enroll and leave) |
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
| Tournament | User/participants | N → M | no (participants enroll and leave) |
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
| TOURNAMENT_UNIQUE_AS_PARTICIPANT | Tournament | All entries in `Tournament.participants` have distinct user IDs |
| TOURNAMENT_ENROLL_UNTIL_START_TIME | Tournament | `∀p ∈ Tournament.participants: p.enrollTime < Tournament.startTime` |
| TOURNAMENT_FINAL_AFTER_START | Tournament | `Tournament.lastModifiedTime > prev.startTime ⟹ startTime, endTime, numberOfQuestions, topics, cancelled are unchanged from prev` |
| TOURNAMENT_IS_CANCELED | Tournament | `prev.cancelled == true ⟹ startTime, endTime, numberOfQuestions, topics, cancelled, participants are unchanged from prev` |
| TOURNAMENT_DELETE | Tournament | `Tournament.state == DELETED ⟹ Tournament.participants.isEmpty()` |
| TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY | Tournament | `∀p ∈ participants where p.userId == creator.userId: p.name == creator.name ∧ p.username == creator.username ∧ p.version == creator.version` |

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

#### Rule: CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT

| Field | Value |
|---|---|
| Entities | Course, Execution, Question |
| Predicate | `Course.executionCount == 0 ⟹ Course.questionCount == 0` |

---

#### Rule: TOPIC_BELONGS_TO_QUESTION_COURSE

| Field | Value |
|---|---|
| Entities | Question, Topic, Course |
| Predicate | `∀t ∈ Question.topics: t.courseId == Question.courseId` |

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
| Predicate | At most one QuizAnswer may exist per `(quizId, userId)` pair |

---

#### Rule: QUESTION_ALREADY_ANSWERED

| Field | Value |
|---|---|
| Entities | QuizAnswer, Question |
| Predicate | `∀qa ∈ QuizAnswer.questionAnswers: all questionIds are distinct` |

---

#### Rule: COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz, Execution |
| Predicate | `QuizAnswer.executionId == QuizAnswer.quiz.executionId` |

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

#### Rule: ANSWER_BEFORE_START (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, QuizAnswer |
| Predicate | `now < Tournament.startTime ⟹ ∀p ∈ participants: p.answer.quizAnswerId == null` |

---

#### Rule: CREATOR_IS_NOT_ANONYMOUS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, User |
| Predicate | `Tournament.creator.name ≠ "ANONYMOUS" ∧ Tournament.creator.username ≠ "ANONYMOUS"` |

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
| Predicate | `∀p ∈ Tournament.participants: p ∈ Tournament.execution.students` |

---

#### Rule: TOPIC_COURSE_EXECUTION (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Topic, Execution, Course |
| Predicate | `∀t ∈ Tournament.topics: t.courseId == Tournament.execution.courseId` |

---

#### Rule: QUIZ_COURSE_EXECUTION_CONSISTENCY (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Quiz, Execution |
| Predicate | `Tournament.quiz.executionId == Tournament.executionId` |

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
| Predicate | `Tournament.numberOfQuestions == count(Quiz.questions) ∧ topics(Quiz.questions) ⊇ Tournament.topics` |

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
| Predicate | `∀p ∈ participants: p.answer statistics reflect the actual QuizAnswer` (`numberOfAnswered`, `numberOfCorrect`, `answered`) |

---

## §4 — Functionalities

| Functionality | Primary Aggregate | Other Aggregates | Description |
|---|---|---|---|
| CreateCourse | Course | — | Create a new course |
| UpdateCourse | Course | — | Update course name or type |
| DeleteCourse | Course | — | Soft-delete a course |
| CreateTopic | Topic | Course | Create a topic linked to a course |
| DeleteTopic | Topic | Question, Tournament | Soft-delete a topic and propagate removal to subscribers |
| CreateExecution | Execution | Course | Create a course execution linked to a course |
| UpdateExecution | Execution | — | Update execution acronym or academic term |
| DeleteExecution | Execution | Course | Delete an execution and decrement course counter |
| DisenrollStudent | Execution | QuizAnswer | Remove a student from a course execution |
| EnrollStudentInExecution | Execution | User | Enroll an active user in a course execution |
| UpdateStudentName | Execution | User | Update a student's cached name across the execution and downstream aggregates |
| AnonymizeStudent | Execution | User | Anonymize a student (set name/username to ANONYMOUS) |
| CreateQuestion | Question | Course, Topic | Create a question linked to a course and topics |
| UpdateQuestion | Question | Topic | Update question content or topics |
| DeleteQuestion | Question | Course | Delete a question and decrement course question counter |
| DeleteUser | User | Execution | Soft-delete a user account |
| CreateQuiz | Quiz | Execution, Question | Create a quiz linked to an execution and questions |
| UpdateQuiz | Quiz | — | Update quiz dates or questions (before available date) |
| CreateQuizAnswer | QuizAnswer | Quiz, User, Execution | Record a student's quiz answer |
| AnswerQuestion | QuizAnswer | Tournament | Record a student's answer to one question in a quiz |
| ConcludeQuiz | QuizAnswer | — | Mark a quiz answer session as completed |
| CreateTournament | Tournament | Execution, User, Topic, Quiz | Create a tournament for a course execution (also creates the associated Quiz) |
| AddParticipant | Tournament | Execution, User | Enroll a student as a tournament participant |
| UpdateTournament | Tournament | Quiz | Update tournament timing or topics |
| CancelTournament | Tournament | — | Cancel an open tournament |
| DeleteTournament | Tournament | — | Delete a cancelled or finished tournament |

---
