package pt.ulisboa.tecnico.socialsoftware.blcm.exception;

public enum ErrorMessage {
    INVALID_AGGREGATE_TYPE("Aggregate type %s does not exist"),

    AGGREGATE_DELETED("Aggregate with aggregate id %d already deleted."),

    VERSION_MANAGER_DOES_NOT_EXIST("Version manager does not exist."),


    TOURNAMENT_NOT_FOUND("Tournament with aggregate Id %d does not exist."),
    TOURNAMENT_INVALID("Tournament version with aggregate id %d and version %d breaks invariants."),
    TOURNAMENT_MERGE_FAILURE("Two versions of a tournament with aggregate id %d cannot be merged."),
    TOURNAMENT_MISSING_USER("Tournament requires a user."),
    TOURNAMENT_MISSING_TOPICS("Tournament requires topics."),
    TOURNAMENT_MISSING_START_TIME("Tournament requires a start time."),
    TOURNAMENT_MISSING_END_TIME("Tournament requires an end time."),
    TOURNAMENT_MISSING_NUMBER_OF_QUESTIONS("Tournament requires a number of questions."),
    TOURNAMENT_DELETED("Tournament with aggregate id %d already deleted."),
    TOURNAMENT_PARTICIPANT_NOT_FOUND("User %d is not enrolled in tournament %d"),
    TOURNAMENT_TOPIC_NOT_FOUND("Topic %d is not part of tournament %d."),
    CANNOT_UPDATE_TOURNAMENT("Tournament %d cannot be updated."),
    CANNOT_DELETE_TOURNAMENT("Tournament %d cannot be deleted."),
    QUIZ_NOT_FOUND("Quiz with aggregate Id %d does not exist"),

    QUIZ_DELETED("Quiz with aggregate id %d already deleted."),
    NOT_ENOUGH_QUESTIONS("Not enough questions to generate quiz."),
    QUIZ_MERGE_FAILURE("Two versions of a quiz with aggregate id %d cannot be merged."),

    COURSE_EXECUTION_NOT_FOUND("Course execution with aggregate id %d does not exist."),
    COURSE_EXECUTION_DELETED("Course execution with aggregate id %d already deleted."),
    COURSE_EXECUTION_MISSING_COURSE_ID("Course execution requires a course id."),
    COURSE_EXECUTION_MISSING_ACRONYM("Course execution requires an acronym."),
    COURSE_EXECUTION_MISSING_ACADEMIC_TERM("Course execution requires an academic term."),
    COURSE_EXECUTION_MISSING_END_DATE("Course execution requires an end date."),

    COURSE_EXECUTION_INVALID("Course execution aggregate id %d  and version %d breaks invariants."),

    CANNOT_DELETE_COURSE_EXECUTION("Cannot delete course execution with aggregate id %d."),


    TOPIC_MISSING_NAME("Topic requires a name."),
    TOPIC_MISSING_COURSE("Topic requires a course."),
    TOPIC_NOT_FOUND("Topic with aggregate id %d not found."),
    TOPIC_DELETED("Topic with aggregate id %d already deleted."),


    USER_MISSING_NAME("User requires a name."),
    USER_MISSING_USERNAME("User requires an username."),
    USER_MISSING_ROLE("User requires a role."),

    USER_NOT_FOUND("User with aggregate id %d does not exist."),
    USER_DELETED("User with aggregate id %d alreadt deleted."),
    INACTIVE_USER("Cannot add course execution to inactive user."),
    USER_ACTIVE("User %d is already active."),

    USER_MERGE_FAILURE("Two versions of a user with aggregate id %d cannot be merged."),

    COURSE_MISSING_TYPE("Course requires a type."),
    COURSE_MISSING_NAME("Course requires a name."),
    COURSE_NOT_FOUND("Course with aggregate id %d not found."),
    COURSE_DELETED("Course with aggregate id %d already deleted."),
    COURSE_INVALID("Course version with aggregate id %d and version %d breaks invariants."),

    QUESTION_NOT_FOUND("Question with aggregate id %d does no exist."),

    QUESTION_DELETED("Question with aggregate id %d already deleted."),

    QUIZ_ANSWER_NOT_FOUND("Answer with aggregate id %d not found."),

    NO_USER_ANSWER_FOR_QUIZ("Answer for user aggregate id %d and quiz aggregate id %d not found."),


    QUIZ_ANSWER_DELETED("Answer with aggregate id %d already deleted."),

    QUESTION_ALREADY_ANSWERED("Question %d of quiz %d already answered."),

    INVALID_OPTION_SELECTED("Invalid option %d for question %d."),

    CANNOT_PERFORM_CAUSAL_READ("Cannot causally read object with aggregate id %d."),
    INVALID_PREV("Prev does not match the type of the aggregate."),
    NO_PRIMARY_AGGREGATE_FOUND("No primary aggregate was found within the transactional context."),
    TOO_MANY_PRIMARY_AGGREGATE_FOUND("More than one primary aggregates were found within the transactional context"),
    INVARIANT_BREAK("Aggregate %d breaks invariants");

    public final String label;

    ErrorMessage(String label) {
        this.label = label;
    }
}
