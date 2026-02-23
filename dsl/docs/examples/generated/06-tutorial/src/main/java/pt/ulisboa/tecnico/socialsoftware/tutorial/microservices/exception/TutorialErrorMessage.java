package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception;

public final class TutorialErrorMessage {
    private TutorialErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String BOOK_MISSING_TITLE = "Book requires a title.";

    public static final String BOOK_MISSING_AUTHOR = "Book requires a author.";

    public static final String BOOK_MISSING_GENRE = "Book requires a genre.";

    public static final String MEMBER_MISSING_NAME = "Member requires a name.";

    public static final String MEMBER_MISSING_EMAIL = "Member requires a email.";

}