package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception;

public final class QuizzesFullErrorMessage {
    private QuizzesFullErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String AGGREGATE_BEING_USED_IN_OTHER_SAGA = "Aggregate is being used in %s saga";

    public static final String INVALID_AGGREGATE_TYPE = "Aggregate type %s does not exist";

    public static final String AGGREGATE_DELETED = "Aggregate %s with aggregate id %d already deleted.";
    public static final String AGGREGATE_NOT_FOUND = "Aggregate with aggregate id %d does not exist.";

    public static final String VERSION_MANAGER_DOES_NOT_EXIST = "Version manager does not exist.";

    public static final String AGGREGATE_MERGE_FAILURE = "Two versions of aggregate %d cannot be merged.";

    // Course
    public static final String COURSE_NOT_FOUND = "Course with aggregate id %d not found.";
    public static final String COURSE_DELETED = "Course with aggregate id %d already deleted.";
    public static final String COURSE_MISSING_NAME = "Course requires a name.";
    public static final String COURSE_MISSING_TYPE = "Course requires a type.";

    // User
    public static final String USER_NOT_FOUND = "User with aggregate id %d does not exist.";
    public static final String USER_DELETED = "User with aggregate id %d already deleted.";
    public static final String USER_MISSING_NAME = "User requires a name.";
    public static final String USER_MISSING_USERNAME = "User requires a username.";
    public static final String USER_MISSING_ROLE = "User requires a role.";
    public static final String INACTIVE_USER = "Cannot add course execution to inactive user.";
    public static final String USER_IS_ANONYMOUS = "Cannot add anonymous user %d.";

    // Topic
    public static final String TOPIC_NOT_FOUND = "Topic with aggregate id %d not found.";
    public static final String TOPIC_DELETED = "Topic with aggregate id %d already deleted.";
    public static final String TOPIC_MISSING_NAME = "Topic requires a name.";
    public static final String TOPIC_MISSING_COURSE = "Topic requires a course.";

    // CourseExecution
    public static final String COURSE_EXECUTION_NOT_FOUND = "Course execution with aggregate id %d does not exist.";
    public static final String COURSE_EXECUTION_DELETED = "Course execution with aggregate id %d already deleted.";
    public static final String COURSE_EXECUTION_MISSING_ACRONYM = "Course execution requires an acronym.";
    public static final String COURSE_EXECUTION_MISSING_ACADEMIC_TERM = "Course execution requires an academic term.";
    public static final String COURSE_EXECUTION_MISSING_END_DATE = "Course execution requires an end date.";
    public static final String COURSE_EXECUTION_STUDENT_ALREADY_ENROLLED = "Student with aggregate id %d is already enrolled in course execution %d.";
    public static final String COURSE_EXECUTION_STUDENT_NOT_FOUND = "Student with aggregate id %d not found in course execution %d.";
    public static final String DUPLICATE_COURSE_EXECUTION = "Course execution with acronym %s and academic term %s already exists.";
    public static final String CANNOT_DELETE_COURSE_EXECUTION_WITH_QUIZZES_OR_ASSESSMENTS = "Cannot delete course execution %d that still has quizzes or assessments.";
    public static final String CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT = "Cannot delete the last course execution %d of a course that still has questions.";
    public static final String REMOVE_NO_STUDENTS = "Cannot delete course execution %d that still has enrolled students.";

    // Question
    public static final String QUESTION_NOT_FOUND = "Question with aggregate id %d does not exist.";
    public static final String QUESTION_DELETED = "Question with aggregate id %d already deleted.";
    public static final String QUESTION_MISSING_TITLE = "Question requires a title.";
    public static final String QUESTION_MISSING_CONTENT = "Question requires content.";
    public static final String QUESTION_TOPIC_INVALID_COURSE = "Topic %d does not belong to course %d.";
    public static final String CANNOT_CHANGE_ANSWERED_QUESTION = "Cannot change question %d that has already been answered.";
    public static final String QUESTION_IS_USED_IN_QUIZ = "Cannot remove question %d that is used in a quiz.";

    // Quiz
    public static final String QUIZ_NOT_FOUND = "Quiz with aggregate id %d does not exist.";
    public static final String QUIZ_DELETED = "Quiz with aggregate id %d already deleted.";
    public static final String QUIZ_MISSING_TITLE = "Quiz requires a title.";
    public static final String QUIZ_MISSING_AVAILABLE_DATE = "Quiz requires an available date.";
    public static final String QUIZ_DATE_ORDERING = "Quiz available date must be before conclusion date which must be before results date.";
    public static final String QUIZ_CANNOT_HAVE_REPEATED_QUESTIONS = "Quiz %d cannot have repeated questions.";
    public static final String QUIZ_HAS_ANSWERS = "Cannot remove quiz %d that has answers.";
    public static final String QUIZ_HAS_TOURNAMENT = "Cannot remove quiz %d that belongs to a tournament.";
    public static final String CANNOT_UPDATE_QUIZ = "Quiz %d cannot be updated after available date.";

    // QuizAnswer
    public static final String QUIZ_ANSWER_NOT_FOUND = "Quiz answer with aggregate id %d not found.";
    public static final String QUIZ_ANSWER_DELETED = "Quiz answer with aggregate id %d already deleted.";
    public static final String QUIZ_ALREADY_STARTED_BY_STUDENT = "Student %d already has an answer for quiz %d.";

    // Tournament
    public static final String TOURNAMENT_NOT_FOUND = "Tournament with aggregate id %d does not exist.";
    public static final String TOURNAMENT_DELETED = "Tournament with aggregate id %d already deleted.";
    public static final String TOURNAMENT_MISSING_START_TIME = "Tournament requires a start time.";
    public static final String TOURNAMENT_MISSING_END_TIME = "Tournament requires an end time.";
    public static final String TOURNAMENT_MISSING_NUMBER_OF_QUESTIONS = "Tournament requires a number of questions.";
    public static final String TOURNAMENT_MAX_QUESTIONS = "Tournament cannot have more than 30 questions.";
    public static final String TOURNAMENT_START_BEFORE_END = "Tournament start time must be before end time.";
    public static final String CANNOT_ADD_PARTICIPANT = "Cannot add participant to tournament %d after it has ended.";
    public static final String TOURNAMENT_CANCELED = "Tournament %d is cancelled.";
    public static final String DUPLICATE_TOURNAMENT_PARTICIPANT = "User %d is already a participant in tournament %d.";
    public static final String PARTICIPANT_NOT_ENROLLED_IN_TOURNAMENT_EXECUTION = "User %d not enrolled in tournament's %d course execution.";
    public static final String CREATOR_IS_ANONYMOUS = "Cannot create tournament with anonymous creator.";
    public static final String TOURNAMENT_PRIVATE_WRONG_PASSWORD = "Incorrect password for private tournament %d.";
    public static final String CANNOT_UPDATE_TOURNAMENT = "Tournament %d cannot be updated after it has started and has answers.";

    // Assessment
    public static final String ASSESSMENT_NOT_FOUND = "Assessment with aggregate id %d does not exist.";
    public static final String ASSESSMENT_DELETED = "Assessment with aggregate id %d already deleted.";
    public static final String ASSESSMENT_MISSING_TITLE = "Assessment requires a title.";

    // Discussion
    public static final String DISCUSSION_NOT_FOUND = "Discussion with aggregate id %d does not exist.";
    public static final String DISCUSSION_DELETED = "Discussion with aggregate id %d already deleted.";
    public static final String DISCUSSION_MISSING_MESSAGE = "Discussion requires a message.";
    public static final String DUPLICATE_DISCUSSION = "A discussion already exists for question answer %d.";
    public static final String DISCUSSION_CLOSE_REQUIRES_REPLY = "Cannot close discussion %d that has no replies.";
    public static final String REPLY_MISSING_MESSAGE = "Reply requires a message.";
    public static final String REPLY_UNAUTHORIZED_USER = "User %d is not authorized to reply to discussion %d.";

    // QuestionSubmission
    public static final String QUESTION_SUBMISSION_NOT_FOUND = "Question submission with aggregate id %d does not exist.";
    public static final String QUESTION_SUBMISSION_DELETED = "Question submission with aggregate id %d already deleted.";
    public static final String DUPLICATE_QUESTION_SUBMISSION = "A submission already exists for question %d by student %d.";
    public static final String QUESTION_SUBMISSION_COURSE_MATCH = "Question %d does not belong to the course of course execution %d.";

    // Dashboard
    public static final String DASHBOARD_NOT_FOUND = "Dashboard with aggregate id %d does not exist.";
    public static final String DASHBOARD_DELETED = "Dashboard with aggregate id %d already deleted.";
    public static final String DIFFICULT_QUESTION_PERCENTAGE_RANGE = "Difficult question percentage must be between 0 and 24.";
    public static final String WEEKLY_SCORE_CANNOT_CLOSE_CURRENT_WEEK = "Cannot close weekly score for current week.";
    public static final String FAILED_ANSWER_COMPLETED_AND_WRONG = "Failed answer can only be created for completed quiz answers with incorrect question answers.";

    // General
    public static final String INVARIANT_BREAK = "Aggregate %d breaks invariants";
    public static final String CANNOT_PERFORM_CAUSAL_READ = "Cannot causally read object with aggregate id %d.";
    public static final String INVALID_EVENT_TYPE = "Invalid event type %s.";
    public static final String CANNOT_MODIFY_INACTIVE_AGGREGATE = "Cannot update aggregate %d because it is INACTIVE.";
    public static final String NOT_ENOUGH_QUESTIONS = "Not enough questions to generate quiz.";
}
