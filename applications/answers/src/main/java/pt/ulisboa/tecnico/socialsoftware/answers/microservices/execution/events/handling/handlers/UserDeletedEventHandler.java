package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.ExecutionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.UserDeletedEvent;

public class UserDeletedEventHandler extends ExecutionEventHandler {
    public UserDeletedEventHandler(ExecutionRepository executionRepository, ExecutionEventProcessing executionEventProcessing) {
        super(executionRepository, executionEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.executionEventProcessing.processUserDeletedEvent(subscriberAggregateId, (UserDeletedEvent) event);
    }
}
