package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.events.CourseDeletedEvent;

public class CourseDeletedEventHandler extends ExecutionEventHandler {
    public CourseDeletedEventHandler(ExecutionRepository executionRepository, ExecutionEventProcessing executionEventProcessing) {
        super(executionRepository, executionEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.executionEventProcessing.processCourseDeletedEvent(subscriberAggregateId, (CourseDeletedEvent) event);
    }
}
