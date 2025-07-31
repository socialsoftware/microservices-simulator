package pt.ulisboa.tecnico.socialsoftware.quizzes;

public enum ServiceMapping {
    TOURNAMENT("tournament"),
    QUIZ("quiz"),
    USER("user"),
    COURSE_EXECUTION("courseExecution"),
    COURSE("course"),
    QUESTION("question"),
    TOPIC("topic");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public static ServiceMapping fromServiceName(String serviceName) {
        for (ServiceMapping mapping : values()) {
            if (mapping.getServiceName().equals(serviceName)) {
                return mapping;
            }
        }
        throw new IllegalArgumentException("Unknown service name: " + serviceName);
    }
}
