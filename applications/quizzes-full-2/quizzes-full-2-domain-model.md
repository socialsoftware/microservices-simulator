# Quizzes — Domain Model (the plain domain)

> Follows the structure defined in [`docs/templates/domain-model-template.md`](../../docs/templates/domain-model-template.md).

<!-- plain-domain: allow — provenance note; it must name what moved out of this file -->
> **Provenance note — 2026-09-19.** This file and
> [`quizzes-full-2-aggregate-grouping.md`](quizzes-full-2-aggregate-grouping.md) were re-partitioned
> after the run that produced this application, so that the pair is one *plain domain* plus one
> *aggregate grouping* over it. Every decomposition decision — aggregate membership, snapshot value
> objects and their cached fields, technical fields, the event DAG, the consistency policy and how
> each cross-entity rule is realised — moved out of this file and into the grouping. **Nothing the
> pair specifies changed**: the union of the two files is semantically identical to the union of
> their pre-rewrite versions, which git holds. The application was not regenerated.
<!-- plain-domain: end -->

**How to use this file:**
1. Read §1–§2 to understand the entities, their attributes, and how they relate.
2. Read §3 to understand every consistency rule and its predicate, stated as a standing invariant over the domain.
3. See [`quizzes-full-2-aggregate-grouping.md`](quizzes-full-2-aggregate-grouping.md) for the concrete partitioning decision, its consistency policy, and how each rule below is realised under it.

---

## §1 — Entities

Each entity lists only its own scalar attributes. Cross-entity references appear in §2.

| Entity | Attributes |
|---|---|
| **Course** | `name: String` (immutable), `type: CourseType (TECNICO \| EXTERNAL)` (immutable) |
| **User** | `name: String`, `username: String`, `role: Role (STUDENT \| TEACHER \| ADMIN)` (immutable), `active: Boolean` (default: false) |
| **Topic** | `name: String` |
| **Execution** | `acronym: String`, `academicTerm: String`, `endDate: LocalDateTime` |
| **Question** | `title: String`, `content: String`, `creationDate: LocalDateTime` |
| **Option** | `sequence: Integer`, `optionKey: Integer`, `content: String`, `correct: Boolean` |
| **Quiz** | `title: String`, `creationDate: LocalDateTime` (immutable), `availableDate: LocalDateTime`, `conclusionDate: LocalDateTime`, `resultsDate: LocalDateTime`, `quizType: QuizType (EXAM \| TEST \| GENERATED \| PROPOSED \| IN_CLASS \| EXTERNAL_QUIZ)` |
| **QuizAnswer** | `creationDate: LocalDateTime` (immutable), `answerDate: LocalDateTime` (immutable), `completed: Boolean` |
| **QuestionAnswer** | `optionSequenceChoice: Integer`, `optionKey: Integer`, `correct: Boolean`, `timeTaken: Integer` |
| **Tournament** | `startTime: LocalDateTime`, `endTime: LocalDateTime`, `numberOfQuestions: Integer`, `cancelled: Boolean` |
| **TournamentParticipant** | `enrollTime: LocalDateTime` |

> **`TournamentParticipant` is an associative entity.** `enrollTime` is an attribute of the
> *participation* — neither of the `Tournament` nor of the `User`. It therefore gets its own row here
> and two `N → 1` relationships in §2, one of them a composition.

> **A participant's answer statistics are derived, not stored.** Whether a participant has answered,
> how many questions they answered, how many they got right and when they first answered are
> quantities over that participant's `QuizAnswer` for the tournament's `Quiz`. §3.2's
> `QUIZ_ANSWER_EXISTS` and `TOURNAMENT_ANSWER_BEFORE_START` refer to them; no entity above declares
> them as attributes.

---

## §2 — Relationships

The direction is always from the referencing entity to the referenced entity. **Immutable** means the
reference is set at creation and never changed. **Composition** means the referencing entity has no
independent existence and is destroyed with its target.

| From | To | Cardinality | Immutable | Composition |
|---|---|---|---|---|
| Topic | Course | N → 1 | yes | no |
| Execution | Course | N → 1 | yes | no |
| Execution | User (students) | N → M | no (students enroll and are disenrolled) | no |
| Question | Course | N → 1 | yes | no |
| Question | Topic | N → M | no (topics can be updated) | no |
| Option | Question | N → 1 | yes | yes |
| Quiz | Execution | N → 1 | yes | no |
| Quiz | Question | N → M | no (frozen after `availableDate`) | no |
| QuizAnswer | Quiz | N → 1 | yes | no |
| QuizAnswer | User/student | N → 1 | yes | no |
| QuizAnswer | Execution | N → 1 | yes | no |
| QuestionAnswer | QuizAnswer | N → 1 | yes | yes |
| QuestionAnswer | Question | N → 1 | yes | no |
| QuestionAnswer | Option (chosen) | N → 1 | no (the student may change the answer) | no |
| Tournament | Execution | N → 1 | yes | no |
| Tournament | User/creator | N → 1 | yes | no |
| Tournament | Topic | N → M | no (frozen after `startTime`) | no |
| Tournament | Quiz | 1 → 1 | yes | no |
| TournamentParticipant | Tournament | N → 1 | yes | yes |
| TournamentParticipant | User | N → 1 | yes | no |
| TournamentParticipant | QuizAnswer | N → 1 | no (set when the participant first answers) | no |

> `Tournament.participants` below denotes the set of `TournamentParticipant`s whose `tournament` is
> that `Tournament`. A participant is removed when the student is disenrolled from the execution, and
> the whole set is cleared when the tournament is deleted.

---

## §3 — Rules

### 3.1 — Single-entity rules

These rules inspect only fields of a single entity.

| Rule | Entity | Predicate |
|---|---|---|
| COURSE_TYPE_FINAL | Course | `Course.type` is immutable (Java `final` field) |
| COURSE_NAME_FINAL | Course | `Course.name` is immutable (Java `final` field) |
| USER_ROLE_FINAL | User | `User.role` is immutable (Java `final` field) |
| USER_DELETED_STATE | User | A User that has been deleted has `active == false` |
| QUIZ_CREATION_DATE_FINAL | Quiz | `Quiz.creationDate` is immutable (Java `final` field) |
| QUIZ_DATE_ORDERING | Quiz | `creationDate < availableDate < conclusionDate ≤ resultsDate` |
| QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE | Quiz | Once `availableDate` has passed, `availableDate`, `conclusionDate`, `resultsDate` and the set of questions do not change |
| TOURNAMENT_START_BEFORE_END_TIME | Tournament | `Tournament.startTime < Tournament.endTime` |
| TOURNAMENT_FINAL_AFTER_START | Tournament | Once `startTime` has passed, `startTime`, `endTime`, `numberOfQuestions`, the set of topics and `cancelled` do not change |

> **Immutability fields:** `TOURNAMENT_CREATOR_IS_FINAL`, `TOURNAMENT_COURSE_EXECUTION_IS_FINAL`, `TOURNAMENT_QUIZ_IS_FINAL`, `QUIZ_COURSE_EXECUTION_FINAL`, `QUIZANSWER_FINAL_USER`, `QUIZANSWER_FINAL_QUIZ`, `QUIZANSWER_FINAL_COURSE_EXECUTION`, `QUIZANSWER_FINAL_CREATION_DATE` are all enforced by Java `final` fields or by absence of setters after construction. No runtime check is needed.

> **The five `TOURNAMENT_*` participant rules are in §3.2, not here.** `TOURNAMENT_UNIQUE_AS_PARTICIPANT`,
> `TOURNAMENT_ENROLL_UNTIL_START_TIME`, `TOURNAMENT_DELETE`, `TOURNAMENT_IS_CANCELED` and
> `TOURNAMENT_ANSWER_BEFORE_START` all relate a `Tournament` to its `TournamentParticipant`s, which
> are entities of their own (§1). They were single-entity rules only while participation was modelled
> as something owned by the tournament, which is a partitioning decision and not a domain fact.

---

### 3.2 — Cross-entity rules

Every rule below is a **standing invariant over the domain**: a statement that is either true or
false of a domain state, with no claim about when or how it is checked. Whether a given realisation
maintains it continuously, checks it once when the operation runs, or tolerates its later violation
is recorded in §5 of
[`quizzes-full-2-aggregate-grouping.md`](quizzes-full-2-aggregate-grouping.md).

"`X` has been deleted" is the domain's notion of removal. A deleted entity is still referenceable but
no longer counts as existing.

---

#### Rule: REMOVE_NO_STUDENTS

| Field | Value |
|---|---|
| Entities | Execution, User |
| Predicate | An Execution that has been deleted has no enrolled students |

---

#### Rule: NO_DUPLICATE_COURSE_EXECUTION

| Field | Value |
|---|---|
| Entities | Execution |
| Predicate | No two Executions that have not been deleted share the same `(acronym, academicTerm)` pair |

---

#### Rule: INACTIVE_USER

| Field | Value |
|---|---|
| Entities | Execution, User |
| Predicate | `∀s ∈ Execution.students: s.active == true` |

---

#### Rule: STUDENT_ALREADY_ENROLLED

| Field | Value |
|---|---|
| Entities | Execution, User |
| Predicate | A User appears in `Execution.students` at most once |

---

#### Rule: USER_EXISTS (Execution)

| Field | Value |
|---|---|
| Entities | Execution, User |
| Predicate | `∀s ∈ Execution.students: s has not been deleted` |

---

#### Rule: CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT *(deferred)*

> **Intentionally deferred.** This cross-entity rule — "a Course may not hold questions unless it
> has at least one execution" — was originally enforced via `executionCount` / `questionCount`
> counters kept on `Course`. That counter-based approach has been removed; the rule is to be re-implemented
> later by a different mechanism. No counters and no count commands currently enforce it.

---

#### Rule: TOPIC_BELONGS_TO_QUESTION_COURSE

| Field | Value |
|---|---|
| Entities | Question, Topic, Course |
| Predicate | `∀t ∈ Question.topics: t.course == Question.course` |

---

#### Rule: TOPICS_EXIST (Question)

| Field | Value |
|---|---|
| Entities | Question, Topic |
| Predicate | `∀t ∈ Question.topics: t has not been deleted` |

---

#### Rule: QUESTION_EXISTS (Quiz)

| Field | Value |
|---|---|
| Entities | Quiz, Question |
| Predicate | `∀q ∈ Quiz.questions: q has not been deleted` |

---

#### Rule: COURSE_EXECUTION_EXISTS (Quiz)

| Field | Value |
|---|---|
| Entities | Quiz, Execution |
| Predicate | `Quiz.execution has not been deleted` |

---

#### Rule: UNIQUE_QUIZ_ANSWER_PER_STUDENT

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz, User |
| Predicate | At most one QuizAnswer exists per `(quiz, student)` pair |

---

#### Rule: QUESTION_ALREADY_ANSWERED

| Field | Value |
|---|---|
| Entities | QuizAnswer, QuestionAnswer, Question |
| Predicate | `∀qa ∈ QuizAnswer.questionAnswers: all reference distinct Questions` |

---

#### Rule: ANSWER_MATCHES_CORRECT_OPTION

| Field | Value |
|---|---|
| Entities | QuizAnswer, QuestionAnswer, Question, Option |
| Predicate | `∀qa ∈ QuizAnswer.questionAnswers where qa.chosenOption != null: qa.correct == qa.chosenOption.correct` — the chosen Option is one of `qa.question`'s Options, and the answer is correct exactly when that Option is the correct one |

---

#### Rule: COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz, Execution |
| Predicate | `QuizAnswer.execution == QuizAnswer.quiz.execution` |

---

#### Rule: USER_EXISTS (QuizAnswer)

| Field | Value |
|---|---|
| Entities | QuizAnswer, User, Execution |
| Predicate | `QuizAnswer.student` has not been deleted and is still enrolled in `QuizAnswer.execution` |

---

#### Rule: QUIZ_EXISTS (QuizAnswer)

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz, Question |
| Predicate | `QuizAnswer.quiz` has not been deleted, and no Question of that Quiz has been deleted — a Quiz that has lost a question is unusable and counts as deleted for every rule downstream of it |

---

#### Rule: COURSE_EXECUTION_EXISTS (QuizAnswer)

| Field | Value |
|---|---|
| Entities | QuizAnswer, Execution |
| Predicate | `QuizAnswer.execution has not been deleted` |

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
| Entities | Tournament, TournamentParticipant, User, Execution |
| Predicate | `∀p ∈ Tournament.participants: p.user ∈ Tournament.execution.students` |

---

#### Rule: TOPIC_COURSE_EXECUTION (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Topic, Execution, Course |
| Predicate | `∀t ∈ Tournament.topics: t.course == Tournament.execution.course` |

---

#### Rule: QUIZ_COURSE_EXECUTION_CONSISTENCY (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Quiz, Execution |
| Predicate | `Tournament.quiz.execution == Tournament.execution` |

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
| Entities | Tournament, TournamentParticipant, User |
| Predicate | The creator and every participant reference Users that have not been deleted or anonymized |

---

#### Rule: TOPIC_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Topic |
| Predicate | `∀t ∈ Tournament.topics: t has not been deleted` |

---

#### Rule: QUIZ_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Quiz, Question |
| Predicate | `Tournament.quiz` has not been deleted, and no Question of that Quiz has been deleted |

---

#### Rule: COURSE_EXECUTION_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Execution |
| Predicate | `Tournament.execution has not been deleted` |

---

#### Rule: QUIZ_ANSWER_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, TournamentParticipant, QuizAnswer, QuestionAnswer |
| Predicate | `∀p ∈ Tournament.participants`: whether `p` has answered, how many questions `p` answered, how many `p` answered correctly and when `p` first answered are exactly those quantities over `p`'s QuizAnswer for `Tournament.quiz` |

---

#### Rule: TOURNAMENT_UNIQUE_AS_PARTICIPANT

| Field | Value |
|---|---|
| Entities | Tournament, TournamentParticipant, User |
| Predicate | No two entries in `Tournament.participants` reference the same User |

---

#### Rule: TOURNAMENT_ENROLL_UNTIL_START_TIME

| Field | Value |
|---|---|
| Entities | Tournament, TournamentParticipant |
| Predicate | `∀p ∈ Tournament.participants: p.enrollTime < Tournament.startTime` |

---

#### Rule: TOURNAMENT_DELETE

| Field | Value |
|---|---|
| Entities | Tournament, TournamentParticipant |
| Predicate | A Tournament that has been deleted has no participants |

---

#### Rule: TOURNAMENT_IS_CANCELED

| Field | Value |
|---|---|
| Entities | Tournament, TournamentParticipant |
| Predicate | Once a Tournament is `cancelled`, `startTime`, `endTime`, `numberOfQuestions`, the set of topics, `cancelled` and the set of participants do not change |

---

#### Rule: TOURNAMENT_ANSWER_BEFORE_START

| Field | Value |
|---|---|
| Entities | Tournament, TournamentParticipant, QuizAnswer |
| Predicate | `∀p ∈ Tournament.participants`: if `p` has answered, the time of `p`'s first answer is `≥ Tournament.startTime` |

---

## §4 — Functionalities

> This section is a complete inventory of every operation the application exposes, write and read,
> one row per operation regardless of how many entities it touches.
>
> **Other Entities** lists every entity besides the primary one that the operation reads or writes.
> The mapping onto the units of one partitioning — and therefore which operations need cross-boundary
> coordination — is derived by joining these columns against §1 of
> [`quizzes-full-2-aggregate-grouping.md`](quizzes-full-2-aggregate-grouping.md).

| Functionality | Primary Entity | Other Entities | Kind | Description |
|---|---|---|---|---|
| CreateCourse | Course | — | Write | Create a new course. Courses are immutable and are never updated or deleted |
| CreateTopic | Topic | Course | Write | Create a topic linked to a course |
| UpdateTopic | Topic | — | Write | Update the name of an existing topic |
| DeleteTopic | Topic | — | Write | Delete a topic |
| CreateExecution | Execution | Course | Write | Create a course execution linked to a course |
| UpdateExecution | Execution | — | Write | Update execution acronym or academic term |
| DeleteExecution | Execution | — | Write | Delete an execution |
| EnrollStudentInExecution | Execution | User | Write | Enroll an active user in a course execution |
| DisenrollStudent | Execution | — | Write | Remove a student from a course execution |
| CreateUser | User | — | Write | Create a user account (inactive until activated) |
| ActivateUser | User | — | Write | Activate a user account so it can be enrolled in executions |
| UpdateUserName | User | — | Write | Update a user's name |
| AnonymizeUser | User | — | Write | Anonymize a user (set name and username to ANONYMOUS) |
| DeleteUser | User | — | Write | Delete a user account |
| CreateQuestion | Question | Course, Topic, Option | Write | Create a question with its options, linked to a course and topics |
| UpdateQuestion | Question | Topic | Write | Update question title, content or topics |
| DeleteQuestion | Question | — | Write | Delete a question |
| CreateQuiz | Quiz | Execution, Question | Write | Create a quiz linked to an execution and questions |
| UpdateQuiz | Quiz | — | Write | Update quiz dates or questions (before available date) |
| CreateQuizAnswer | QuizAnswer | QuestionAnswer, Quiz, User, Execution, Question | Write | Start a student's answer session for a quiz, seeding one QuestionAnswer per quiz question |
| AnswerQuestion | QuizAnswer | QuestionAnswer | Write | Record a student's answer to one question in a quiz, as the sequence and key of the option chosen |
| ConcludeQuiz | QuizAnswer | — | Write | Mark a quiz answer session as completed |
| CreateTournament | Tournament | Execution, User, Topic, Quiz | Write | Create a tournament for a course execution (also creates the associated Quiz) |
| AddParticipant | Tournament | TournamentParticipant, Execution, User | Write | Enroll a student as a tournament participant |
| UpdateTournament | Tournament | Quiz | Write | Update tournament timing or topics |
| CancelTournament | Tournament | — | Write | Cancel an open tournament |
| DeleteTournament | Tournament | TournamentParticipant | Write | Delete a cancelled or finished tournament, clearing its participant list in the same operation so that TOURNAMENT_DELETE holds |
| GetCourseById | Course | — | Read | Retrieve a single course by its id |
| GetCourses | Course | — | Read | List all courses |
| GetUserById | User | — | Read | Retrieve a single user by its id |
| GetStudents | User | — | Read | List all users with the STUDENT role |
| GetTeachers | User | — | Read | List all users with the TEACHER role |
| GetTopicsByCourse | Topic | — | Read | List the topics belonging to a course |
| GetExecutionById | Execution | — | Read | Retrieve a single course execution by its id |
| GetExecutions | Execution | — | Read | List all course executions |
| GetUserExecutions | Execution | — | Read | List the course executions a given user is enrolled in |
| GetQuestionById | Question | Option | Read | Retrieve a single question by its id |
| GetQuestionsByCourse | Question | Option | Read | List the questions belonging to a course |
| GetQuizById | Quiz | — | Read | Retrieve a single quiz by its id |
| GetQuizzesForExecution | Quiz | — | Read | List the quizzes of a course execution |
| GetQuizAnswerById | QuizAnswer | QuestionAnswer | Read | Retrieve a single quiz answer session by its id |
| GetQuizAnswerForStudentAndQuiz | QuizAnswer | QuestionAnswer, Quiz | Read | Retrieve the quiz answer session of a given student for a given quiz |
| GetTournamentById | Tournament | TournamentParticipant | Read | Retrieve a single tournament by its id |
| GetTournamentsForExecution | Tournament | TournamentParticipant | Read | List all tournaments of a course execution |
| GetOpenedTournamentsForExecution | Tournament | TournamentParticipant | Read | List the tournaments of a course execution that are currently open |
| GetClosedTournamentsForExecution | Tournament | TournamentParticipant | Read | List the tournaments of a course execution that have closed |

---
