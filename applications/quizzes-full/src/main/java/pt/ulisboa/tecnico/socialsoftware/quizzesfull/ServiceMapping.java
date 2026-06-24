package pt.ulisboa.tecnico.socialsoftware.quizzesfull;

public enum ServiceMapping {
    COURSE("course"),
    USER("user"),
    TOPIC("topic"),
    COURSE_EXECUTION("courseExecution"),
    QUESTION("question"),
    QUIZ("quiz"),
    QUIZ_ANSWER("quizAnswer"),
    TOURNAMENT("tournament"),
    ASSESSMENT("assessment"),
    DISCUSSION("discussion"),
    QUESTION_SUBMISSION("questionSubmission"),
    DASHBOARD("dashboard");

    private final String serviceName;

    ServiceMapping(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
