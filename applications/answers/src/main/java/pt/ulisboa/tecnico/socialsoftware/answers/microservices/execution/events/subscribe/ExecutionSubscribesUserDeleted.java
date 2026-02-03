package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.UserDeletedEvent;

public class ExecutionSubscribesUserDeleted extends EventSubscription {
    

    public ExecutionSubscribesUserDeleted( ) {
        super(.getAggregateId(),
                0,
                UserDeletedEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
