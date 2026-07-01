package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentQuiz;

public class TournamentSubscribesInvalidateQuiz extends EventSubscription {
    public TournamentSubscribesInvalidateQuiz(TournamentQuiz tournamentQuiz) {
        super(tournamentQuiz.getQuizAggregateId(),
                tournamentQuiz.getQuizVersion(),
                InvalidateQuizEvent.class.getSimpleName());
    }

    public TournamentSubscribesInvalidateQuiz() {}

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}