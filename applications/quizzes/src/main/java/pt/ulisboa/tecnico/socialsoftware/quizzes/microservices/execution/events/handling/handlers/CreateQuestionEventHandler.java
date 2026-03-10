package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.CreateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing;

public class CreateQuestionEventHandler extends CourseExecutionEventHandler {
    public CreateQuestionEventHandler(CourseExecutionRepository courseExecutionRepository,
            ExecutionEventProcessing executionEventProcessing) {
        super(courseExecutionRepository, executionEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.executionEventProcessing.processCreateQuestionEvent(subscriberAggregateId, (CreateQuestionEvent) event);
    }
}
