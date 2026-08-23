package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception;

public final class TrainticketErrorMessage {
    private TrainticketErrorMessage() {}

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

    public static final String STATION_STAY_TIME_NON_NEGATIVE = "Station stay time must be non-negative.";
    public static final String DUPLICATE_STATION_NAME = "Another active station already holds this name.";

    public static final String TRAIN_TYPE_SEATS_NON_NEGATIVE = "Train type seat counts must be non-negative.";
    public static final String TRAIN_TYPE_HAS_SEATS = "Train type must have at least one seat.";
    public static final String TRAIN_TYPE_SPEED_POSITIVE = "Train type average speed must be positive.";

    public static final String USER_DOCUMENT_NUMBER_PRESENT = "User document number must be present when a document type is set.";
    public static final String DUPLICATE_USER_NAME = "Another active user already holds this user name.";

    public static final String ROUTE_HAS_AT_LEAST_TWO_STATIONS = "Route must have at least two stations.";
    public static final String ROUTE_SEQUENCE_CONTIGUOUS = "Route station sequences must be contiguous and start at zero.";
    public static final String ROUTE_DISTANCES_MONOTONIC = "Route station distances must strictly increase along the sequence.";
    public static final String ROUTE_FIRST_DISTANCE_IS_ZERO = "The first route station must be at distance zero.";
    public static final String ROUTE_STATIONS_DISTINCT = "Route stations must reference distinct stations.";
    public static final String ROUTE_ENDPOINTS_MATCH_STATION_LIST = "Route start and end station names must match the first and last stations of its list.";

    public static final String CONTACTS_DOCUMENT_NUMBER_PRESENT = "Contacts document number must be present when a document type is set.";

    public static final String TRIP_START_BEFORE_END = "Trip start time must be before its end time.";
    public static final String DUPLICATE_TRIP_NUMBER = "Another active trip already holds this trip number.";

    public static final String PRICE_RATES_POSITIVE = "Price configuration rates must be positive.";

    public static final String PLACEHOLDER = "placeholder";
}
