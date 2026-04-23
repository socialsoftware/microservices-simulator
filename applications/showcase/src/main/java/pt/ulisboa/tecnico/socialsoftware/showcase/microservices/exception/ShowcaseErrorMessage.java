package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception;

public final class ShowcaseErrorMessage {
    private ShowcaseErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String AGGREGATE_BEING_USED_IN_OTHER_SAGA = "Aggregate is being used in %s saga";

    public static final String INVALID_AGGREGATE_TYPE = "Aggregate type %s does not exist";

    public static final String AGGREGATE_DELETED = "Aggregate %s with aggregate id %d already deleted.";

    public static final String AGGREGATE_NOT_FOUND = "Aggregate with aggregate id %d does not exist.";

    public static final String VERSION_MANAGER_DOES_NOT_EXIST = "Version manager does not exist.";

    public static final String AGGREGATE_MERGE_FAILURE = "Two versions of aggregate %d cannot be merged.";

    public static final String AGGREGATE_MERGE_FAILURE_DUE_TO_INTENSIONS_CONFLICT = "Two versions of aggregate cannot be merged due to intensions conflict: %s";

    public static final String INVARIANT_BREAK = "Aggregate %d breaks invariants";

    public static final String CANNOT_MODIFY_INACTIVE_AGGREGATE = "Cannot update aggregate %d because it is INACTIVE.";

    public static final String INVALID_EVENT_TYPE = "Invalid event type %s.";

    public static final String BOOKING_MISSING_CHECKINDATE = "Booking requires a checkInDate.";

    public static final String BOOKING_MISSING_CHECKOUTDATE = "Booking requires a checkOutDate.";

    public static final String ROOM_MISSING_ROOMNUMBER = "Room requires a roomNumber.";

    public static final String ROOM_MISSING_DESCRIPTION = "Room requires a description.";

    public static final String ROOM_MISSING_AMENITIES = "Room requires a amenities.";

    public static final String USER_MISSING_USERNAME = "User requires a username.";

    public static final String USER_MISSING_EMAIL = "User requires a email.";

}