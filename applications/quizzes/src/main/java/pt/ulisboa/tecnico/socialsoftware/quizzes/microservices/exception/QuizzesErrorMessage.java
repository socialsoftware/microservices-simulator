package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception;

public final class QuizzesErrorMessage {
    private QuizzesErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String AGGREGATE_BEING_USED_IN_OTHER_SAGA = "Aggregate is being used in %s saga";

    public static final String INVALID_AGGREGATE_TYPE = "Aggregate type %s does not exist";

    public static final String AGGREGATE_DELETED = "Aggregate %s with aggregate id %d already deleted.";
    public static final String AGGREGATE_NOT_FOUND = "Aggregate with aggregate id %d does not exist.";

    public static final String VERSION_MANAGER_DOES_NOT_EXIST = "Version manager does not exist.";

    public static final String AGGREGATE_MERGE_FAILURE = "Two versions of aggregate %d cannot be merged.";
    public static final String AGGREGATE_MERGE_FAILURE_DUE_TO_INTENSIONS_CONFLICT = "Two versions of aggregate cannot be merged due to intensions conflict: %s";

    public static final String TOURNAMENT_NOT_FOUND = "Tournament with aggregate Id %d does not exist.";
    public static final String TOURNAMENT_INVALID = "Tournament version with aggregate id %d and version %d breaks invariants.";
    public static final String TOURNAMENT_MISSING_USER = "Tournament requires a user.";
    public static final String TOURNAMENT_MISSING_TOPICS = "Tournament requires topics.";
    public static final String TOURNAMENT_MISSING_START_TIME = "Tournament requires a start time.";
    public static final String TOURNAMENT_MISSING_END_TIME = "Tournament requires an end time.";
    public static final String TOURNAMENT_MISSING_NUMBER_OF_QUESTIONS = "Tournament requires a number of questions.";
    public static final String TOURNAMENT_DELETED = "Tournament with aggregate id %d already deleted.";
    public static final String TOURNAMENT_PARTICIPANT_NOT_FOUND = "User %d is not enrolled in tournament %d";
    public static final String TOURNAMENT_TOPIC_NOT_FOUND = "Topic %d is not part of tournament %d.";
    public static final String CANNOT_UPDATE_TOURNAMENT = "Tournament %d cannot be updated.";
    public static final String CANNOT_DELETE_TOURNAMENT = "Tournament %d cannot be deleted.";
    public static final String QUIZ_NOT_FOUND = "Quiz with aggregate Id %d does not exist.";
    public static final String CANNOT_ADD_PARTICIPANT = "Cannot add participant to tournament %d after it has started.";
    public static final String PARTICIPANT_NOT_STUDENT = "User %d must be a student to be added as participant to tournament %d.";
    public static final String PARTICIPANT_NOT_ENROLLED_IN_TOURNAMENT_EXECUTION = "User %d not enrolled in tournament's %d course execution.";
    public static final String TOURNAMENT_PARTICIPANT_ADDING_ANSWER_WITH_WRONG_QUIZ_ANSWER_ID = "Tournament participant is being added a wrong quiz answer id %d";
    public static final String TOURNAMENT_IN_SAGA = "Tournament is already in a saga.";

    public static final String COURSE_EXECUTION_STUDENT_ALREADY_ENROLLED = "Student with aggregate id %d is already enrolled in course execution %d.";

    public static final String USER_IS_ANONYMOUS = "Cant add anonymous user %d.";
    public static final String CREATOR_IS_ANONYMOUS = "Cant add user %d because creator is anonymous.";

    public static final String QUIZ_DELETED = "Quiz with aggregate id %d already deleted.";
    public static final String NOT_ENOUGH_QUESTIONS = "Not enough questions to generate quiz.";
    public static final String QUIZ_MERGE_FAILURE = "Two versions of a quiz with aggregate id %d cannot be merged.";

    public static final String CANNOT_UPDATE_QUIZ = "Quiz %d cannot be deleted.";

    public static final String COURSE_EXECUTION_NOT_FOUND = "Course execution with aggregate id %d does not exist.";
    public static final String COURSE_EXECUTION_DELETED = "Course execution with aggregate id %d already deleted.";
    public static final String COURSE_EXECUTION_MISSING_COURSE_ID = "Course execution requires a course id.";
    public static final String COURSE_EXECUTION_MISSING_ACRONYM = "Course execution requires an acronym.";
    public static final String COURSE_EXECUTION_MISSING_ACADEMIC_TERM = "Course execution requires an academic term.";
    public static final String COURSE_EXECUTION_MISSING_END_DATE = "Course execution requires an end date.";

    public static final String COURSE_EXECUTION_INVALID = "Course execution aggregate id %d  and version %d breaks invariants.";
    public static final String CANNOT_DELETE_COURSE_EXECUTION = "Cannot delete course execution with aggregate id %d.";
    public static final String COURSE_EXECUTION_STUDENT_NOT_FOUND = "Student with aggregate id %d not found in course execution %d.";


    public static final String TOPIC_MISSING_NAME = "Topic requires a name.";
    public static final String TOPIC_MISSING_COURSE = "Topic requires a course.";
    public static final String TOPIC_NOT_FOUND = "Topic with aggregate id %d not found.";
    public static final String TOPIC_DELETED = "Topic with aggregate id %d already deleted.";


    public static final String USER_MISSING_NAME = "User requires a name.";
    public static final String USER_MISSING_USERNAME = "User requires an username.";
    public static final String USER_MISSING_ROLE = "User requires a role.";

    public static final String USER_NOT_FOUND = "User with aggregate id %d does not exist.";
    public static final String USER_DELETED = "User with aggregate id %d alreadt deleted.";
    public static final String INACTIVE_USER = "Cannot add course execution to inactive user.";
    public static final String USER_ACTIVE = "User %d is already active.";
    public static final String USER_NOT_ACTIVE = "User %d is already not active.";

    public static final String USER_MERGE_FAILURE = "Two versions of a user with aggregate id %d cannot be merged.";

    public static final String COURSE_MISSING_TYPE = "Course requires a type.";
    public static final String COURSE_MISSING_NAME = "Course requires a name.";
    public static final String COURSE_NOT_FOUND = "Course with aggregate id %d not found.";
    public static final String COURSE_DELETED = "Course with aggregate id %d already deleted.";
    public static final String COURSE_INVALID = "Course version with aggregate id %d and version %d breaks invariants.";

    public static final String QUESTION_NOT_FOUND = "Question with aggregate id %d does no exist.";

    public static final String QUESTION_DELETED = "Question with aggregate id %d already deleted.";
    public static final String QUESTION_TOPIC_INVALID_COURSE = "Topic %d does not belong to course %d.";

    public static final String QUIZ_ANSWER_NOT_FOUND = "Answer with aggregate id %d not found.";

    public static final String NO_USER_ANSWER_FOR_QUIZ = "Answer for user aggregate id %d and quiz aggregate id %d not found.";

    public static final String QUIZ_ANSWER_DELETED = "Answer with aggregate id %d already deleted.";

    public static final String QUIZ_DOES_NOT_BELONG_TO_COURSE_EXECUTION = "Quiz %d does not belong to course execution %d.";

    public static final String QUESTION_ALREADY_ANSWERED = "Question %d of quiz %d already answered.";

    public static final String INVALID_OPTION_SELECTED = "Invalid option %d for question %d.";

    public static final String CANNOT_PERFORM_CAUSAL_READ = "Cannot causally read object with aggregate id %d.";
    public static final String CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED = "Cannot causally read object of class %s to causal snapshot because emitted event %s was not processed";
    public static final String INVALID_PREV = "Prev does not match the type of the aggregate.";
    public static final String NO_PRIMARY_AGGREGATE_FOUND = "No primary aggregate was found within the transactional context.";
    public static final String TOO_MANY_PRIMARY_AGGREGATE_FOUND = "More than one primary aggregates were found within the transactional context";
    public static final String INVARIANT_BREAK = "Aggregate %d breaks invariants";
    public static final String INVALID_EVENT_TYPE = "Invalid event type %s.";
    public static final String CANNOT_MODIFY_INACTIVE_AGGREGATE = "Cannot update aggregate %d because it is INACTIVE.";
}
