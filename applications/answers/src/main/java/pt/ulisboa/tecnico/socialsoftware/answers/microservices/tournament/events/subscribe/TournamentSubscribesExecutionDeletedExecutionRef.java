package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionDeletedEvent;


public class TournamentSubscribesExecutionDeletedExecutionRef extends EventSubscription {
    public TournamentSubscribesExecutionDeletedExecutionRef(TournamentExecution execution) {
        super(execution.getExecutionAggregateId(),
                execution.getExecutionVersion(),
                ExecutionDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
