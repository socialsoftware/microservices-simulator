package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.exception;

public final class HelloworldErrorMessage {
    private HelloworldErrorMessage() {}

    public static final String UNDEFINED_TRANSACTIONAL_MODEL = "Undefined transactional model";

    public static final String TASK_MISSING_TITLE = "Task requires a title.";

    public static final String TASK_MISSING_DESCRIPTION = "Task requires a description.";

}