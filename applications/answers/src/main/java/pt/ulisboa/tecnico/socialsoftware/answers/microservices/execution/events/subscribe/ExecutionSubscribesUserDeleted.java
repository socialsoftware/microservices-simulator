package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserDeletedEvent;

public class ExecutionSubscribesUserDeleted extends EventSubscription {
    

    public ExecutionSubscribesUserDeleted() {
        // Parameterless constructor for simple subscriptions
        // Event matching is handled by the framework
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
