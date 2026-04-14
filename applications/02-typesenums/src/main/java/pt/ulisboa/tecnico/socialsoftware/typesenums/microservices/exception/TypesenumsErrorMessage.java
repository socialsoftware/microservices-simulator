package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.exception;

public final class TypesenumsErrorMessage {
    private TypesenumsErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String CONTACT_MISSING_FIRSTNAME = "Contact requires a firstName.";

    public static final String CONTACT_MISSING_LASTNAME = "Contact requires a lastName.";

    public static final String CONTACT_MISSING_EMAIL = "Contact requires a email.";

}