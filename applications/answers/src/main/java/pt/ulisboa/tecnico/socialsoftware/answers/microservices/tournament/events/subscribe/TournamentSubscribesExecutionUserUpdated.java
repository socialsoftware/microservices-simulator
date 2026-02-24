package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUserUpdatedEvent;

public class TournamentSubscribesExecutionUserUpdated extends EventSubscription {
    

    public TournamentSubscribesExecutionUserUpdated(TournamentCreator tournamentCreator) {
        super(tournamentCreator.getCreatorAggregateId(),
                tournamentCreator.getCreatorVersion(),
                ExecutionUserUpdatedEvent.class.getSimpleName());
        
    }

    public TournamentSubscribesExecutionUserUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
