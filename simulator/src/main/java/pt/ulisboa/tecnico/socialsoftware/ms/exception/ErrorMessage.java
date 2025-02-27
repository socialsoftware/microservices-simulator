package pt.ulisboa.tecnico.socialsoftware.ms.exception;

public enum ErrorMessage {
    UNDEFINED_TRANSACTIONAL_MODEL("Undefined transactional model"),

    AGGREGATE_BEING_USED_IN_OTHER_SAGA("Aggregate is being used in %s saga"),

    INVALID_AGGREGATE_TYPE("Aggregate type %s does not exist"),

    AGGREGATE_DELETED("Aggregate %s with aggregate id %d already deleted."),
    AGGREGATE_NOT_FOUND("Aggregate with aggregate id %d does not exist."),

    VERSION_MANAGER_DOES_NOT_EXIST("Version manager does not exist."),

    AGGREGATE_MERGE_FAILURE("Two versions of aggregate %d cannot be merged."),
    AGGREGATE_MERGE_FAILURE_DUE_TO_INTENSIONS_CONFLICT("Two versions of aggregate cannot be merged due to intensions conflict: %s"),


    CANNOT_PERFORM_CAUSAL_READ("Cannot causally read object with aggregate id %d."),
    CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED("Cannot causally read object of class %s to causal snapshot because emitted event %s was not processed"),
    INVALID_PREV("Prev does not match the type of the aggregate."),
    NO_PRIMARY_AGGREGATE_FOUND("No primary aggregate was found within the transactional context."),
    TOO_MANY_PRIMARY_AGGREGATE_FOUND("More than one primary aggregates were found within the transactional context"),
    INVARIANT_BREAK("Aggregate %d breaks invariants"),
    INVALID_EVENT_TYPE("Invalid event type %s."),
    CANNOT_MODIFY_INACTIVE_AGGREGATE("Cannot update aggregate %d because it is INACTIVE.");

    public final String label;

    ErrorMessage(String label) {
        this.label = label;
    }
}
