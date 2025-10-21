package pt.ulisboa.tecnico.socialsoftware.quizzes;

public enum ServiceMapping {
    TOURNAMENT("tournament"),
    QUIZ("quiz"),
    USER("user"),
    COURSE_EXECUTION("courseExecution"),
    COURSE("course"),
    QUESTION("question"),
    TOPIC("topic"),
    ANSWER("answer");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
