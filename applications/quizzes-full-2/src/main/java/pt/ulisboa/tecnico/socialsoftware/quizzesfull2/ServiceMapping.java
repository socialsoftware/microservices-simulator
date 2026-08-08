package pt.ulisboa.tecnico.socialsoftware.quizzesfull2;

public enum ServiceMapping {
    COURSE("course"),
    USER("user"),
    TOPIC("topic"),
    EXECUTION("execution"),
    QUESTION("question"),
    QUIZ("quiz"),
    QUIZ_ANSWER("quizAnswer"),
    TOURNAMENT("tournament"),
    ;

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
