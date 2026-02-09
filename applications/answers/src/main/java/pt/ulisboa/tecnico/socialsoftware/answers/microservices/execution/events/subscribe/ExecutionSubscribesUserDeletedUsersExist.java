package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserDeletedEvent;


public class ExecutionSubscribesUserDeletedUsersExist extends EventSubscription {
    public ExecutionSubscribesUserDeletedUsersExist(ExecutionUser users) {
        super(users.getUserAggregateId(),
                users.getUserVersion(),
                UserDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
