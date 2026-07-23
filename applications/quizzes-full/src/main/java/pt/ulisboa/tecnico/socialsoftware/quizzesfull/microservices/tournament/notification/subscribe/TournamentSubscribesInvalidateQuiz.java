package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.Tournament;

public class TournamentSubscribesInvalidateQuiz extends EventSubscription {

    public TournamentSubscribesInvalidateQuiz(Tournament tournament) {
        super(tournament.getQuizAggregateId(), tournament.getQuizVersion(),
                InvalidateQuizEvent.class.getSimpleName());
    }

    public TournamentSubscribesInvalidateQuiz() {}
}
