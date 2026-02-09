package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;


public class TournamentSubscribesExecutionDeletedTournamentExecutionExists extends EventSubscription {
    public TournamentSubscribesExecutionDeletedTournamentExecutionExists(TournamentExecution execution) {
        super(execution.getExecutionAggregateId(),
                execution.getExecutionVersion(),
                ExecutionDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
