# Quizzes-Full — Domain Model

> Follows the structure defined in [`docs/templates/domain-model-template.md`](../../docs/templates/domain-model-template.md).
>
> Source: extracted from [`quizzes-tutor/quizzes-tutor-domain-model.md`](../../../quizzes-tutor/quizzes-tutor-domain-model.md).
> Compare with the simplified simulator model at [`applications/quizzes/quizzes-domain-model.md`](../quizzes/quizzes-domain-model.md).

**How to use this file:**
1. Read §1–§2 to understand the entities, their attributes, and how they relate.
2. Read §3 to understand every consistency rule and its predicate.
3. See [`quizzes-full-aggregate-grouping.md`](quizzes-full-aggregate-grouping.md) for the concrete aggregate partitioning decision and its event-dependency consequences.

---

## §1 — Entities

Each entity lists only its own scalar attributes. Cross-entity references appear in §2. The **Owns** column lists value objects that live inside this entity's boundary and have no independent identity (they are created and deleted with the entity).

| Entity | Attributes | Owns |
|---|---|---|
| **Course** | `name: String` (immutable), `type: CourseType` (TECNICO \| EXTERNAL) (immutable) | — |
| **User** | `key: Integer` (immutable), `name: String`, `username: String`, `role: Role` (STUDENT \| TEACHER \| ADMIN \| DEMO_ADMIN) (immutable), `active: Boolean` | — |
| **Topic** | `name: String` | — |
| **CourseExecution** | `acronym: String`, `academicTerm: String`, `status: Status` (ACTIVE \| INACTIVE \| HISTORIC), `endDate: LocalDateTime`, `type: CourseType` | — |
| **Question** | `key: Integer` (immutable), `title: String`, `content: String`, `status: Status` (DISABLED \| REMOVED \| AVAILABLE \| SUBMITTED), `creationDate: LocalDateTime` (immutable), `numberOfAnswers: Integer` (cached counter), `numberOfCorrect: Integer` (cached counter) | Option × N, Image × 1 |
| **Option** | `sequence: Integer`, `content: String`, `correct: Boolean` | — |
| **Image** | `url: String`, `width: Integer` | — |
| **Quiz** | `key: Integer` (immutable), `title: String`, `creationDate: LocalDateTime` (immutable), `availableDate: LocalDateTime`, `conclusionDate: LocalDateTime`, `resultsDate: LocalDateTime`, `quizType: QuizType` (EXAM \| TEST \| GENERATED \| PROPOSED \| IN_CLASS \| TOURNAMENT), `scramble: Boolean`, `qrCodeOnly: Boolean`, `oneWay: Boolean`, `lastModifiedTime: LocalDateTime` (technical) | QuizQuestion × N |
| **QuizQuestion** | `sequence: Integer` | — |
| **QuizAnswer** | `creationDate: LocalDateTime` (immutable), `answerDate: LocalDateTime`, `completed: Boolean`, `fraud: Boolean`, `usedInStatistics: Boolean`, `currentSequenceQuestion: Integer` | QuestionAnswer × N |
| **QuestionAnswer** | `sequence: Integer`, `timeTaken: Integer`, `correct: Boolean` | — |
| **Tournament** | `startTime: LocalDateTime`, `endTime: LocalDateTime`, `numberOfQuestions: Integer`, `cancelled: Boolean`, `privateTournament: Boolean`, `password: String`, `lastModifiedTime: LocalDateTime` (technical) | — |
| **Assessment** | `title: String`, `sequence: Integer`, `status: Status` (DISABLED \| AVAILABLE \| REMOVED) | TopicConjunction × N |
| **TopicConjunction** | *(no scalar attributes; identity derived from its topic set)* | — |
| **Discussion** | `message: String`, `date: LocalDateTime` (immutable), `closed: Boolean` | Reply × N |
| **Reply** | `message: String`, `date: LocalDateTime` (immutable), `isPublic: Boolean` | — |
| **QuestionSubmission** | `status: Status` (IN_REVIEW \| IN_REVISION \| APPROVED \| REJECTED), `studentRead: Boolean`, `teacherRead: Boolean` | Review × N |
| **Review** | `comment: String`, `creationDate: LocalDateTime` (immutable), `type: Type` (APPROVE \| REJECT \| REQUEST_CHANGES \| REQUEST_REVIEW \| COMMENT) | — |
| **Dashboard** | `numberOfTeacherQuizzes: Integer`, `numberOfStudentQuizzes: Integer`, `numberOfInClassQuizzes: Integer`, `numberOfTeacherAnswers: Integer`, `numberOfInClassAnswers: Integer`, `numberOfStudentAnswers: Integer`, `numberOfCorrectTeacherAnswers: Integer`, `numberOfCorrectInClassAnswers: Integer`, `numberOfCorrectStudentAnswers: Integer`, `lastCheckFailedAnswers: LocalDateTime`, `lastCheckWeeklyScores: LocalDateTime` | WeeklyScore × N, FailedAnswer × N |
| **WeeklyScore** | `week: LocalDate` (immutable), `quizzesAnswered: Integer`, `questionsAnswered: Integer`, `questionsUniquelyAnswered: Integer`, `percentageCorrect: Integer`, `improvedCorrectAnswers: Integer`, `closed: Boolean` | — |
| **FailedAnswer** | `collected: LocalDateTime` (immutable), `answered: Boolean` | — |
| **DifficultQuestion** | `percentage: Integer` (0–24) | — |

> **Question-type hierarchy:** `Option × N` covers the `MultipleChoiceQuestion` subtype. `CodeOrderQuestion` and `CodeFillInQuestion` subtypes own `CodeOrderSlot × N` and `CodeFillInSpot × N` respectively. For simplicity this model treats all subtypes uniformly; the subtype-specific owned children are elided.
>
> **Answer-type hierarchy:** `QuestionAnswer` holds subtype-specific answer details (`MultipleChoiceAnswer`, `CodeOrderAnswer`, `CodeFillInAnswer`) as an owned value. The `correct` field above is the resolved correctness across all subtype checks.
>
> **User hierarchy:** `User` is the single entity; `role` discriminates students from teachers. No separate `Student`/`Teacher` entity is needed in the simulator model.
>
> **Technical field note:** `lastModifiedTime` on `Quiz` and `Tournament` is stamped at mutation time so that `verifyInvariants()` can check temporal constraints (e.g., "fields are final after `availableDate`") without calling `now()`, keeping the invariant idempotent across TCC merges.
>
> **Cached counters:** `Question.numberOfAnswers` and `Question.numberOfCorrect` are maintained by events from QuizAnswer. They gate the CANNOT_CHANGE_ANSWERED_QUESTION rule without a cross-service query.

---

## §2 — Relationships

The direction is always from the referencing entity to the referenced entity. **Immutable** means the reference is set at creation and never changed.

| From | To | Cardinality | Immutable |
|---|---|---|---|
| Topic | Course | N → 1 | yes |
| CourseExecution | Course | N → 1 | yes |
| CourseExecution | User (students) | N → M | no (students enroll and leave) |
| Question | Course | N → 1 | yes |
| Question | Topic | N → M | no (topics can be updated) |
| Assessment | CourseExecution | N → 1 | yes |
| Assessment | Topic (via TopicConjunction) | N → M | no (can be updated while DISABLED) |
| Quiz | CourseExecution | N → 1 | yes |
| QuizQuestion | Question | N → 1 | yes |
| QuizAnswer | Quiz | N → 1 | yes |
| QuizAnswer | User/student | N → 1 | yes |
| QuestionAnswer | QuizQuestion | N → 1 | yes |
| Tournament | CourseExecution | N → 1 | yes |
| Tournament | User/creator | N → 1 | yes |
| Tournament | User/participants | N → M | no (participants join and leave) |
| Tournament | Topic | N → M | no (modifiable until `startTime`) |
| Tournament | Quiz | 1 → 1 | yes |
| Discussion | QuestionAnswer | 1 → 1 | yes |
| Discussion | User/student | N → 1 | yes |
| Discussion | Question | N → 1 | yes |
| Discussion | CourseExecution | N → 1 | yes |
| Reply | User | N → 1 | yes |
| QuestionSubmission | Question | 1 → 1 | yes |
| QuestionSubmission | User/student (submitter) | N → 1 | yes |
| QuestionSubmission | CourseExecution | N → 1 | yes |
| Review | User | N → 1 | yes |
| DifficultQuestion | Question | N → 1 | yes |
| DifficultQuestion | CourseExecution | N → 1 | yes |
| Dashboard | User/student | 1 → 1 | yes |
| Dashboard | CourseExecution | N → 1 | yes |
| FailedAnswer | QuestionAnswer | 1 → 1 | yes |

---

## §3 — Rules

### 3.1 — Single-entity rules

These rules inspect only fields of a single entity.

| Rule | Entity | Predicate |
|---|---|---|
| COURSE_NAME_FINAL | Course | `Course.name` is immutable (Java `final` field) |
| COURSE_TYPE_FINAL | Course | `Course.type` is immutable (Java `final` field) |
| COURSE_NAME_NOT_BLANK | Course | `Course.name != null && !blank` |
| COURSE_TYPE_NOT_NULL | Course | `Course.type != null` |
| USER_KEY_FINAL | User | `User.key` is immutable (Java `final` field) |
| USER_ROLE_FINAL | User | `User.role` is immutable (Java `final` field) |
| USER_ROLE_NOT_NULL | User | `User.role != null` |
| USER_DELETED_STATE | User | `User.state == DELETED ⟹ User.active == false` |
| QUIZ_KEY_FINAL | Quiz | `Quiz.key` is immutable (Java `final` field) |
| QUIZ_CREATION_DATE_FINAL | Quiz | `Quiz.creationDate` is immutable (Java `final` field) |
| QUIZ_TITLE_NOT_BLANK | Quiz | `Quiz.title != null && !blank` |
| QUIZ_AVAILABLE_DATE_NOT_NULL | Quiz | `Quiz.availableDate != null` |
| QUIZ_DATE_ORDERING | Quiz | `availableDate ≤ conclusionDate ≤ resultsDate` (conclusionDate required for IN_CLASS) |
| QUIZ_QUESTION_SEQUENCE_STARTS_AT_ONE | Quiz | Sequence of QuizQuestions starts at 1 and is contiguous (no gaps) |
| QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE | Quiz | `Quiz.lastModifiedTime > prev.availableDate ⟹ availableDate, conclusionDate, resultsDate, QuizQuestions are unchanged from prev` |
| QUESTION_KEY_FINAL | Question | `Question.key` is immutable (Java `final` field) |
| QUESTION_CREATION_DATE_FINAL | Question | `Question.creationDate` is immutable (Java `final` field) |
| QUESTION_TITLE_NOT_BLANK | Question | `Question.title != null && !blank` |
| QUESTION_CONTENT_NOT_BLANK | Question | `Question.content != null && !blank` |
| OPTION_SEQUENCE_NON_NEGATIVE | Option | `Option.sequence != null && >= 0` |
| OPTION_CONTENT_NOT_BLANK | Option | `Option.content != null && !blank` |
| ONE_CORRECT_OPTION_NEEDED | Option (MultipleChoice) | Exactly one Option in the owning Question has `correct == true` |
| IMAGE_URL_NOT_BLANK | Image | `Image.url != null && !blank` |
| QUIZ_ANSWER_SEQUENCE_NON_NEGATIVE | QuestionAnswer | `QuestionAnswer.sequence != null && >= 0` |
| TOURNAMENT_START_BEFORE_END | Tournament | `Tournament.startTime < Tournament.endTime` |
| TOURNAMENT_NUMBER_OF_QUESTIONS_POSITIVE | Tournament | `Tournament.numberOfQuestions > 0` |
| TOURNAMENT_MAX_QUESTIONS | Tournament | `Tournament.numberOfQuestions <= 30` |
| TOURNAMENT_UNIQUE_AS_PARTICIPANT | Tournament | All entries in `Tournament.participants` have distinct user IDs |
| TOURNAMENT_FINAL_AFTER_START | Tournament | `Tournament.lastModifiedTime > prev.startTime ⟹ startTime, endTime, numberOfQuestions, topics, cancelled are unchanged from prev` |
| TOURNAMENT_IS_CANCELED | Tournament | `prev.cancelled == true ⟹ startTime, endTime, numberOfQuestions, topics, cancelled, participants are unchanged from prev` |
| TOURNAMENT_ENROLL_UNTIL_END | Tournament | `Tournament.addParticipant() ⟹ now() ≤ Tournament.endTime` |
| DIFFICULT_QUESTION_PERCENTAGE_RANGE | DifficultQuestion | `0 <= DifficultQuestion.percentage <= 24` |
| ASSESSMENT_TITLE_NOT_BLANK | Assessment | `Assessment.title != null && !blank` |
| DISCUSSION_MESSAGE_NOT_BLANK | Discussion | `Discussion.message != null && !blank` |
| DISCUSSION_DATE_FINAL | Discussion | `Discussion.date` is immutable (Java `final` field) |
| REPLY_MESSAGE_NOT_BLANK | Reply | `Reply.message != null && !blank` |
| REPLY_DATE_FINAL | Reply | `Reply.date` is immutable (Java `final` field) |
| REVIEW_COMMENT_NOT_BLANK | Review | `Review.comment != null && !blank` |
| WEEKLY_SCORE_CANNOT_CLOSE_CURRENT_WEEK | WeeklyScore | `WeeklyScore.week != currentWeek` when closing |
| COURSE_EXECUTION_ACRONYM_NOT_BLANK | CourseExecution | `CourseExecution.acronym != null && !blank` |
| COURSE_EXECUTION_ACADEMIC_TERM_NOT_BLANK | CourseExecution | `CourseExecution.academicTerm != null && !blank` |

> **Immutability fields enforced by `final` / absence of setters (no `verifyInvariants()` check needed):** `QUIZ_COURSE_EXECUTION_FINAL`, `QUIZANSWER_FINAL_QUIZ`, `QUIZANSWER_FINAL_USER`, `QUIZANSWER_FINAL_CREATION_DATE`, `TOURNAMENT_QUIZ_IS_FINAL`, `TOURNAMENT_CREATOR_IS_FINAL`, `TOURNAMENT_COURSE_EXECUTION_IS_FINAL`, `DISCUSSION_QUESTION_ANSWER_FINAL`, `DISCUSSION_STUDENT_FINAL`, `DISCUSSION_COURSE_EXECUTION_FINAL`, `QUESTION_SUBMISSION_QUESTION_FINAL`, `QUESTION_SUBMISSION_STUDENT_FINAL`, `QUESTION_SUBMISSION_COURSE_EXECUTION_FINAL`, `FAILED_ANSWER_QUESTION_ANSWER_FINAL`, `DASHBOARD_USER_FINAL`, `DASHBOARD_COURSE_EXECUTION_FINAL`.

---

### 3.2 — Cross-entity rules

---

#### Rule: NO_DUPLICATE_COURSE_EXECUTION

| Field | Value |
|---|---|
| Entities | Course, CourseExecution |
| Predicate | No two CourseExecutions belonging to the same Course share the same `(acronym, academicTerm)` pair |

---

#### Rule: CANNOT_DELETE_COURSE_EXECUTION_WITH_QUIZZES_OR_ASSESSMENTS

| Field | Value |
|---|---|
| Entities | CourseExecution, Quiz, Assessment |
| Predicate | `CourseExecution.remove() ⟹ CourseExecution.quizzes.isEmpty() ∧ CourseExecution.assessments.isEmpty()` |

---

#### Rule: CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT

| Field | Value |
|---|---|
| Entities | Course, CourseExecution, Question |
| Predicate | Deleting the last CourseExecution of a Course is only allowed if the Course has no Questions |

---

#### Rule: REMOVE_NO_STUDENTS

| Field | Value |
|---|---|
| Entities | CourseExecution, User |
| Predicate | `CourseExecution.state == DELETED ⟹ CourseExecution.students.isEmpty()` |

---

#### Rule: INACTIVE_USER

| Field | Value |
|---|---|
| Entities | CourseExecution, User |
| Predicate | A User being enrolled in a CourseExecution must have `active == true` |

---

#### Rule: STUDENT_ALREADY_ENROLLED

| Field | Value |
|---|---|
| Entities | CourseExecution, User |
| Predicate | A User may appear in `CourseExecution.students` at most once |

---

#### Rule: USER_EXISTS (CourseExecution)

| Field | Value |
|---|---|
| Entities | CourseExecution, User |
| Predicate | `∀s ∈ CourseExecution.students: s references a User that has not been deleted` |

---

#### Rule: USER_IS_ACTIVE (removal guard)

| Field | Value |
|---|---|
| Entities | User, CourseExecution |
| Predicate | `User.remove() ⟹ User is not enrolled in any active CourseExecution` |

---

#### Rule: USER_HAS_REPLIES

| Field | Value |
|---|---|
| Entities | User, Reply |
| Predicate | `User.remove() ⟹ User has no Reply records` |

---

#### Rule: STUDENT_HAS_QUIZ_ANSWERS

| Field | Value |
|---|---|
| Entities | User, QuizAnswer |
| Predicate | `User.remove() ⟹ User.quizAnswers.isEmpty()` |

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

#### Rule: CANNOT_CHANGE_ANSWERED_QUESTION

| Field | Value |
|---|---|
| Entities | Question, QuizAnswer |
| Predicate | `Question.update() ⟹ Question.numberOfAnswers == 0` |

---

#### Rule: QUESTION_IS_USED_IN_QUIZ

| Field | Value |
|---|---|
| Entities | Question, QuizQuestion |
| Predicate | `Question.remove() ⟹ ∄ QuizQuestion referencing this Question` |

---

#### Rule: COURSE_EXECUTION_EXISTS (Quiz)

| Field | Value |
|---|---|
| Entities | Quiz, CourseExecution |
| Predicate | `Quiz.courseExecution references a CourseExecution that has not been deleted` |

---

#### Rule: QUIZ_CANNOT_HAVE_REPEATED_QUESTIONS

| Field | Value |
|---|---|
| Entities | Quiz, Question |
| Predicate | All QuizQuestions in a Quiz reference distinct Questions (no duplicates) |

---

#### Rule: QUESTION_EXISTS (Quiz)

| Field | Value |
|---|---|
| Entities | Quiz, Question |
| Predicate | `∀qq ∈ Quiz.quizQuestions: qq references a Question that has not been deleted` |

---

#### Rule: QUIZ_QUESTION_HAS_ANSWERS

| Field | Value |
|---|---|
| Entities | QuizQuestion, QuestionAnswer |
| Predicate | `QuizQuestion.remove() ⟹ QuizQuestion.questionAnswers.isEmpty()` |

---

#### Rule: QUIZ_HAS_ANSWERS

| Field | Value |
|---|---|
| Entities | Quiz, QuizAnswer |
| Predicate | `Quiz.remove() ⟹ Quiz.quizAnswers.isEmpty()` |

---

#### Rule: QUIZ_HAS_TOURNAMENT

| Field | Value |
|---|---|
| Entities | Quiz, Tournament |
| Predicate | `Quiz.remove() ⟹ Quiz.tournament == null` |

---

#### Rule: UNIQUE_QUIZ_ANSWER_PER_STUDENT

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz, User |
| Predicate | At most one QuizAnswer may exist per `(quizId, userId)` pair |

---

#### Rule: ONE_WAY_QUIZ_SEQUENCE

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz, QuestionAnswer |
| Predicate | For `Quiz.oneWay == true`: a QuestionAnswer may only be submitted for the question at index `currentSequenceQuestion`; earlier answers cannot be changed |

---

#### Rule: QUIZ_EXISTS (QuizAnswer)

| Field | Value |
|---|---|
| Entities | QuizAnswer, Quiz |
| Predicate | `QuizAnswer.quiz references a Quiz that has not been invalidated` |

---

#### Rule: USER_EXISTS (QuizAnswer)

| Field | Value |
|---|---|
| Entities | QuizAnswer, User |
| Predicate | `QuizAnswer.student references a User that has not been deleted` |

---

#### Rule: TOURNAMENT_NOT_OPEN

| Field | Value |
|---|---|
| Entities | Tournament |
| Predicate | `Tournament.addParticipant() ⟹ now() ≤ Tournament.endTime` |

---

#### Rule: TOURNAMENT_CANCELED

| Field | Value |
|---|---|
| Entities | Tournament |
| Predicate | `Tournament.addParticipant() ⟹ Tournament.cancelled == false` |

---

#### Rule: DUPLICATE_TOURNAMENT_PARTICIPANT

| Field | Value |
|---|---|
| Entities | Tournament, User |
| Predicate | A User may appear in `Tournament.participants` at most once |

---

#### Rule: PARTICIPANT_COURSE_EXECUTION (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, User, CourseExecution |
| Predicate | `∀p ∈ Tournament.participants: p ∈ Tournament.courseExecution.students` |

---

#### Rule: CREATOR_COURSE_EXECUTION (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, User, CourseExecution |
| Predicate | `Tournament.creator ∈ Tournament.courseExecution.students` |

---

#### Rule: CREATOR_IS_NOT_ANONYMOUS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, User |
| Predicate | `Tournament.creator.name ≠ "ANONYMOUS" ∧ Tournament.creator.username ≠ "ANONYMOUS"` |

---

#### Rule: TOURNAMENT_PRIVATE_PASSWORD

| Field | Value |
|---|---|
| Entities | Tournament |
| Predicate | `Tournament.privateTournament == true ⟹ enrolling participant must supply the correct password` |

---

#### Rule: TOPIC_COURSE_EXECUTION (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Topic, CourseExecution, Course |
| Predicate | `∀t ∈ Tournament.topics: t.courseId == Tournament.courseExecution.courseId` |

---

#### Rule: TOPIC_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Topic |
| Predicate | `∀t ∈ Tournament.topics: t references a Topic that has not been deleted` |

---

#### Rule: QUIZ_COURSE_EXECUTION_CONSISTENCY (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, Quiz, CourseExecution |
| Predicate | `Tournament.quiz.courseExecutionId == Tournament.courseExecutionId` |

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
| Predicate | `Tournament.numberOfQuestions == count(Quiz.quizQuestions) ∧ topics(Quiz.questions) ⊇ Tournament.topics` |

---

#### Rule: CREATOR_EXISTS / PARTICIPANT_EXISTS (Tournament)

| Field | Value |
|---|---|
| Entities | Tournament, User |
| Predicate | Creator and all participants reference Users that have not been deleted or anonymized |

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
| Entities | Tournament, CourseExecution |
| Predicate | `Tournament.courseExecution references a CourseExecution that has not been deleted` |

---

#### Rule: TOURNAMENT_CANNOT_CHANGE_WHEN_OPEN

| Field | Value |
|---|---|
| Entities | Tournament, QuizAnswer |
| Predicate | `Tournament.checkCanChange() ⟹ now() < Tournament.startTime ∨ Tournament.quizAnswers.isEmpty()` (tournament open and has answers → immutable) |

---

#### Rule: TOPIC_COURSE_EXECUTION (Assessment)

| Field | Value |
|---|---|
| Entities | Assessment, Topic, CourseExecution, Course |
| Predicate | `∀t ∈ Assessment.topics: t.courseId == Assessment.courseExecution.courseId` |

---

#### Rule: ASSESSMENT_COURSE_EXECUTION_EXISTS

| Field | Value |
|---|---|
| Entities | Assessment, CourseExecution |
| Predicate | `Assessment.courseExecution references a CourseExecution that has not been deleted` |

---

#### Rule: DIFFICULT_QUESTION_COURSE_MATCH

| Field | Value |
|---|---|
| Entities | DifficultQuestion, Question, CourseExecution |
| Predicate | `DifficultQuestion.question.courseId == DifficultQuestion.courseExecution.courseId` |

---

#### Rule: DUPLICATE_DIFFICULT_QUESTION

| Field | Value |
|---|---|
| Entities | DifficultQuestion, Question, CourseExecution |
| Predicate | At most one DifficultQuestion may exist per `(questionId, courseExecutionId)` pair |

---

#### Rule: FAILED_ANSWER_STUDENT_MATCH

| Field | Value |
|---|---|
| Entities | FailedAnswer, Dashboard, QuestionAnswer |
| Predicate | `FailedAnswer.questionAnswer.quizAnswer.student == FailedAnswer.dashboard.student` |

---

#### Rule: FAILED_ANSWER_COURSE_MATCH

| Field | Value |
|---|---|
| Entities | FailedAnswer, Dashboard, QuizAnswer |
| Predicate | `FailedAnswer.questionAnswer.quizAnswer.quiz.courseExecution == FailedAnswer.dashboard.courseExecution` |

---

#### Rule: FAILED_ANSWER_COMPLETED_AND_WRONG

| Field | Value |
|---|---|
| Entities | FailedAnswer, QuizAnswer, QuestionAnswer |
| Predicate | `FailedAnswer creation ⟹ QuizAnswer.completed == true ∧ QuestionAnswer.correct == false` |

---

#### Rule: DUPLICATE_DISCUSSION

| Field | Value |
|---|---|
| Entities | Discussion, QuestionAnswer |
| Predicate | At most one Discussion may exist per QuestionAnswer |

---

#### Rule: QUESTION_ANSWER_HAS_DISCUSSION

| Field | Value |
|---|---|
| Entities | QuestionAnswer, Discussion |
| Predicate | `QuestionAnswer.remove() ⟹ QuestionAnswer.discussion == null` |

---

#### Rule: REPLY_AUTHORIZED_USER

| Field | Value |
|---|---|
| Entities | Reply, User, Discussion |
| Predicate | `Reply.user.role == TEACHER ∨ Reply.user == Discussion.student` |

---

#### Rule: DISCUSSION_CLOSE_REQUIRES_REPLY

| Field | Value |
|---|---|
| Entities | Discussion, Reply |
| Predicate | `Discussion.changeStatus(closed=true) ⟹ Discussion.replies.isEmpty() == false` |

---

#### Rule: QUESTION_SUBMISSION_COURSE_MATCH

| Field | Value |
|---|---|
| Entities | QuestionSubmission, Question, CourseExecution, Course |
| Predicate | `QuestionSubmission.question.courseId == QuestionSubmission.courseExecution.courseId` |

---

#### Rule: DUPLICATE_QUESTION_SUBMISSION

| Field | Value |
|---|---|
| Entities | QuestionSubmission, Question, User |
| Predicate | At most one QuestionSubmission may exist per `(questionId, studentId)` pair |

---

#### Rule: WEEKLY_SCORE_COURSE_MATCH

| Field | Value |
|---|---|
| Entities | WeeklyScore, Dashboard, QuizAnswer |
| Predicate | Quizzes counted in a WeeklyScore belong to `Dashboard.courseExecution` |

---

## §4 — Functionalities

Operations that the application exposes. Each row maps directly to one `/new-functionality` invocation in Phase 3 of `/new-application`.

> **One row per operation.** The **Primary Aggregate** is the one that owns the main state change (the one whose service method is the coordination entry point). List every aggregate that the saga reads or writes in **Other Aggregates**.
> If an operation touches only a single aggregate, omit it here — it becomes a plain service method with no saga coordination.

| Functionality | Primary Aggregate | Other Aggregates | Description |
|---|---|---|---|
| CreateTopic | Topic | Course | Create a topic and link it to a course |
| CreateCourseExecution | CourseExecution | Course | Create a course execution linked to a course |
| DeleteCourseExecution | CourseExecution | Course, Quiz, Assessment | Delete an execution (guards: no quizzes, no assessments, not last execution with questions) |
| EnrollStudentInCourseExecution | CourseExecution | User | Enroll an active user in a course execution |
| UpdateStudentName | User | CourseExecution, QuizAnswer, Tournament, Discussion, QuestionSubmission | Update a student's name and propagate the change to all downstream aggregates |
| AnonymizeStudent | User | CourseExecution, QuizAnswer, Tournament, Discussion, QuestionSubmission | Anonymize a student (set name/username to ANONYMOUS) and propagate to all downstream aggregates |
| CreateQuestion | Question | Course, Topic | Create a question linked to a course and topics |
| UpdateQuestion | Question | Topic | Update question content or topics |
| DeleteQuestion | Question | Course | Delete a question and decrement the course question counter |
| CreateQuiz | Quiz | CourseExecution, Question | Create a quiz linked to a course execution and questions |
| UpdateQuiz | Quiz | Question | Update quiz dates or questions (before available date) |
| CreateQuizAnswer | QuizAnswer | Quiz, User, CourseExecution | Record a student's quiz answer session |
| CreateTournament | Tournament | CourseExecution, User, Topic | Create a tournament for a course execution with selected topics |
| AddParticipant | Tournament | CourseExecution, User | Enroll a student as a tournament participant |
| UpdateTournament | Tournament | Quiz | Update tournament timing or topics |
| CreateAssessment | Assessment | CourseExecution, Topic | Create an assessment for a course execution with topic groups |
| UpdateAssessment | Assessment | Topic | Update assessment topic groups |
| CreateDiscussion | Discussion | CourseExecution, User, QuizAnswer | Create a discussion on a student's question answer |
| AddReply | Discussion | User | Add a reply to a discussion by a teacher or the original student |
| CreateQuestionSubmission | QuestionSubmission | Question, User, CourseExecution | Submit a student-proposed question for teacher review |
| ReviewQuestionSubmission | QuestionSubmission | User | Add a review comment to a question submission |
| ApproveQuestionSubmission | QuestionSubmission | Question | Approve a submission and update the question's status to AVAILABLE |

---
