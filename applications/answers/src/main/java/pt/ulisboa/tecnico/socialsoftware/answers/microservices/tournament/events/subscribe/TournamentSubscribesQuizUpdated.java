package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizUpdatedEvent;

public class TournamentSubscribesQuizUpdated extends EventSubscription {
    

    public TournamentSubscribesQuizUpdated() {
        // Parameterless constructor for simple subscriptions
        // Event matching is handled by the framework
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
