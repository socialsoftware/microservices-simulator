package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain;

public final class QuizzesFull2DomainConstants {
    private QuizzesFull2DomainConstants() {}

    // Written by AnonymizeUser, compared against by Tournament's CREATOR_IS_NOT_ANONYMOUS on its own
    // cached snapshot. Shared here rather than on either aggregate so neither microservice package
    // depends on the other, and so the two sides cannot drift apart silently.
    public static final String ANONYMOUS = "ANONYMOUS";
}
