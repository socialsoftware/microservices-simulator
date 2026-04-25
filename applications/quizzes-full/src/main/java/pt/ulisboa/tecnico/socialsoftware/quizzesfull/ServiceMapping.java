package pt.ulisboa.tecnico.socialsoftware.quizzesfull;

public enum ServiceMapping {
    COURSE("course"),
    USER("user"),
    TOPIC("topic"),
    QUESTION("question"),
    EXECUTION("execution"),
    TOURNAMENT("tournament"),
    QUIZ("quiz"),
    ANSWER("answer");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
