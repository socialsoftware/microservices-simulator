package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.ExecutionDeletedEvent;

public class TournamentSubscribesExecutionDeleted extends EventSubscription {
    

    public TournamentSubscribesExecutionDeleted( ) {
        super(.getCreator().getCreatorAggregateId(),
                .getCreator().getCreatorVersion(),
                ExecutionDeletedEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
