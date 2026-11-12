package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain;

public final class QuizzesFull2DomainConstants {
    private QuizzesFull2DomainConstants() {}

    // Written by AnonymizeUser, compared against by Tournament's CREATOR_IS_NOT_ANONYMOUS on its own
    // cached snapshot. Shared here rather than on either aggregate so neither microservice package
    // depends on the other, and so the two sides cannot drift apart silently.
    public static final String ANONYMOUS = "ANONYMOUS";

    // Written by CreateTournament onto the quiz it generates. Title is not a CreateTournament
    // parameter and Quiz has no default, so the tournament side has to supply one; it lives here
    // rather than inlined in the saga so the two aggregates cannot drift on what a generated quiz
    // is called.
    public static final String TOURNAMENT_QUIZ_TITLE = "Tournament quiz";
}
