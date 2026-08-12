package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUpdatedEvent;

public class TournamentSubscribesExecutionUpdated extends EventSubscription {
    

    public TournamentSubscribesExecutionUpdated(TournamentExecution tournamentExecution) {
        super(tournamentExecution.getExecutionAggregateId(),
                tournamentExecution.getExecutionVersion(),
                ExecutionUpdatedEvent.class.getSimpleName());
        
    }

    public TournamentSubscribesExecutionUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
