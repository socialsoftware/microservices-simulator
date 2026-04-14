package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception;

public final class ShowcaseErrorMessage {
    private ShowcaseErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String BOOKING_MISSING_CHECKINDATE = "Booking requires a checkInDate.";

    public static final String BOOKING_MISSING_CHECKOUTDATE = "Booking requires a checkOutDate.";

    public static final String ROOM_MISSING_ROOMNUMBER = "Room requires a roomNumber.";

    public static final String ROOM_MISSING_DESCRIPTION = "Room requires a description.";

    public static final String ROOM_MISSING_AMENITIES = "Room requires a amenities.";

    public static final String USER_MISSING_USERNAME = "User requires a username.";

    public static final String USER_MISSING_EMAIL = "User requires a email.";

}