package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;

public class TournamentSubscribesQuizDeleted extends EventSubscription {
    public TournamentSubscribesQuizDeleted(Tournament tournament) {
        super(tournament.getAggregateId(), 0, QuizDeletedEvent.class);
    }
}
