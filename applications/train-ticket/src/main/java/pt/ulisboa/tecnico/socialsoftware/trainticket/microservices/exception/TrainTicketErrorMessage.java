package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception;

public final class TrainTicketErrorMessage {
    private TrainTicketErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String CONTACTS_MISSING_NAME = "Contacts requires a name.";

    public static final String CONTACTS_MISSING_DOCUMENTNUMBER = "Contacts requires a documentNumber.";

    public static final String CONTACTS_MISSING_PHONENUMBER = "Contacts requires a phoneNumber.";

    public static final String ORDER_MISSING_BOUGHTDATE = "Order requires a boughtDate.";

    public static final String ORDER_MISSING_TRAVELDATE = "Order requires a travelDate.";

    public static final String ORDER_MISSING_TRAVELTIME = "Order requires a travelTime.";

    public static final String ORDER_MISSING_SEATNUMBER = "Order requires a seatNumber.";

    public static final String PAYMENT_MISSING_PAYMENTDATE = "Payment requires a paymentDate.";

    public static final String ROUTE_MISSING_STATIONS = "Route requires a stations.";

    public static final String STATION_MISSING_NAME = "Station requires a name.";

    public static final String TRAIN_MISSING_NAME = "Train requires a name.";

    public static final String TRIP_MISSING_TRIPNUMBER = "Trip requires a tripNumber.";

    public static final String TRIP_MISSING_STARTTIME = "Trip requires a startTime.";

    public static final String TRIP_MISSING_ENDTIME = "Trip requires a endTime.";

    public static final String USER_MISSING_USERNAME = "User requires a userName.";

    public static final String USER_MISSING_PASSWORD = "User requires a password.";

    public static final String USER_MISSING_DOCUMENTNUM = "User requires a documentNum.";

    public static final String USER_MISSING_EMAIL = "User requires a email.";

}