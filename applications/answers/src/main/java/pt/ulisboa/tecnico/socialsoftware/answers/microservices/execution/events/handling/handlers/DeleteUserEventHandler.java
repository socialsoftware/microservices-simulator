package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.ExecutionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.executionuser.events.publish.DeleteUserEvent;

public class DeleteUserEventHandler extends ExecutionEventHandler {
    public DeleteUserEventHandler(ExecutionRepository executionRepository, ExecutionEventProcessing executionEventProcessing) {
        super(executionRepository, executionEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.executionEventProcessing.processDeleteUserEvent(subscriberAggregateId, (DeleteUserEvent) event);
    }
}
