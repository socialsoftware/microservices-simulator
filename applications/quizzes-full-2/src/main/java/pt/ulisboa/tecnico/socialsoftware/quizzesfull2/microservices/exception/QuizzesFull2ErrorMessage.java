package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception;

public final class QuizzesFull2ErrorMessage {
    private QuizzesFull2ErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String AGGREGATE_BEING_USED_IN_OTHER_SAGA = "Aggregate is being used in %s saga";

    public static final String INVALID_AGGREGATE_TYPE = "Aggregate type %s does not exist";

    public static final String AGGREGATE_DELETED = "Aggregate %s with aggregate id %d already deleted.";
    public static final String AGGREGATE_NOT_FOUND = "Aggregate with aggregate id %d does not exist.";

    public static final String VERSION_MANAGER_DOES_NOT_EXIST = "Version manager does not exist.";

    public static final String AGGREGATE_MERGE_FAILURE = "Two versions of aggregate %d cannot be merged.";
    public static final String AGGREGATE_MERGE_FAILURE_DUE_TO_INTENSIONS_CONFLICT = "Two versions of aggregate cannot be merged due to intensions conflict: %s";

    public static final String CANNOT_PERFORM_CAUSAL_READ = "Cannot causally read object with aggregate id %d.";
    public static final String CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED = "Cannot causally read object of class %s to causal snapshot because emitted event %s was not processed";
    public static final String INVALID_PREV = "Prev does not match the type of the aggregate.";
    public static final String NO_PRIMARY_AGGREGATE_FOUND = "No primary aggregate was found within the transactional context.";
    public static final String TOO_MANY_PRIMARY_AGGREGATE_FOUND = "More than one primary aggregates were found within the transactional context";
    public static final String INVARIANT_BREAK = "Aggregate %d breaks invariants";
    public static final String INVALID_EVENT_TYPE = "Invalid event type %s.";
    public static final String CANNOT_MODIFY_INACTIVE_AGGREGATE = "Cannot update aggregate %d because it is INACTIVE.";

    public static final String PLACEHOLDER = "placeholder";

    public static final String USER_DELETED_STATE = "A deleted user cannot be active";

    public static final String REMOVE_NO_STUDENTS = "A deleted course execution cannot have students";
    public static final String STUDENT_ALREADY_ENROLLED = "The student is already enrolled in the course execution";
    public static final String NO_DUPLICATE_COURSE_EXECUTION = "A course execution with the same acronym and academic term already exists";
    public static final String INACTIVE_USER = "An inactive user cannot be enrolled in a course execution";

    public static final String TOPIC_BELONGS_TO_QUESTION_COURSE = "A question topic must belong to the question's course";

    public static final String QUIZ_DATE_ORDERING = "The quiz dates must be ordered: creation before available, available before conclusion, conclusion not after results";
    public static final String QUIZ_FIELDS_FINAL_AFTER_AVAILABLE_DATE = "The quiz dates and questions cannot be changed after the quiz becomes available";

    public static final String QUESTION_ALREADY_ANSWERED = "The question has already been answered in this quiz answer";
    public static final String ANSWER_MATCHES_CORRECT_OPTION = "An answer is correct if and only if the chosen option key is the question's correct option key";
    public static final String QUIZ_ANSWER_NOT_FOUND = "The student has no quiz answer for this quiz";
    public static final String UNIQUE_QUIZ_ANSWER_PER_STUDENT = "The student already has a quiz answer for this quiz";
    public static final String COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION = "The quiz answer's course execution must be the quiz's course execution";
    public static final String QUESTION_NOT_IN_QUIZ_ANSWER = "The question is not part of this quiz answer";

    public static final String TOURNAMENT_START_BEFORE_END_TIME = "The tournament start time must precede its end time";
    public static final String TOURNAMENT_UNIQUE_AS_PARTICIPANT = "A student can only participate once in a tournament";
    public static final String TOURNAMENT_ENROLL_UNTIL_START_TIME = "A participant must enroll before the tournament starts";
    public static final String TOURNAMENT_ANSWER_BEFORE_START = "A participant cannot answer before the tournament starts";
    public static final String TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY = "The creator's participation must mirror the creator snapshot";
    public static final String TOURNAMENT_FINAL_AFTER_START = "The tournament schedule and topics cannot be changed after the tournament starts";
    public static final String TOURNAMENT_IS_CANCELED = "A cancelled tournament cannot be changed";
    public static final String TOURNAMENT_DELETE = "A deleted tournament cannot have participants";
    public static final String CREATOR_IS_NOT_ANONYMOUS = "An anonymized user cannot be the creator of a tournament";
    public static final String TOPIC_COURSE_EXECUTION = "A tournament topic must belong to the course of the tournament's course execution";
}
