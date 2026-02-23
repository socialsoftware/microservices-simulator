package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.exception;

public final class EventdrivenErrorMessage {
    private EventdrivenErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String AUTHOR_MISSING_NAME = "Author requires a name.";

    public static final String AUTHOR_MISSING_BIO = "Author requires a bio.";

    public static final String POST_MISSING_TITLE = "Post requires a title.";

    public static final String POST_MISSING_CONTENT = "Post requires a content.";

}