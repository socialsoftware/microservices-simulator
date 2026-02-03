package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.ExecutionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserUpdatedEvent;

public class UserUpdatedEventHandler extends ExecutionEventHandler {
    public UserUpdatedEventHandler(ExecutionRepository executionRepository, ExecutionEventProcessing executionEventProcessing) {
        super(executionRepository, executionEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.executionEventProcessing.processUserUpdatedEvent(subscriberAggregateId, (UserUpdatedEvent) event);
    }
}
