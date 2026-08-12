package pt.ulisboa.tecnico.socialsoftware.answers;

public enum ServiceMapping {
    ANSWER("answer"),
    COURSE("course"),
    EXECUTION("execution"),
    QUESTION("question"),
    QUIZ("quiz"),
    TOPIC("topic"),
    TOURNAMENT("tournament"),
    USER("user");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
