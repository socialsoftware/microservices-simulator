package pt.ulisboa.tecnico.socialsoftware.ms.exception;

public final class SimulatorErrorMessage {
    private SimulatorErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model.";

    public static final String AGGREGATE_BEING_USED_IN_OTHER_SAGA = "Aggregate is being used in %s saga.";

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

}
