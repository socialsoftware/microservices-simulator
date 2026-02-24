package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipantQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuizUpdatedEvent;

public class TournamentSubscribesQuizUpdated extends EventSubscription {
    

    public TournamentSubscribesQuizUpdated(TournamentParticipantQuiz tournamentParticipantQuiz) {
        super(tournamentParticipantQuiz.getParticipantQuizAggregateId(),
                tournamentParticipantQuiz.getParticipantQuizVersion(),
                QuizUpdatedEvent.class.getSimpleName());
        
    }

    public TournamentSubscribesQuizUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
