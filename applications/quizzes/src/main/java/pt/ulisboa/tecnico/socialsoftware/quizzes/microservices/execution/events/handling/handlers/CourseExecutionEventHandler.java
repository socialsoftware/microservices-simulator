package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing;

import java.util.Set;

public abstract class CourseExecutionEventHandler extends EventHandler {
    private CourseExecutionRepository courseExecutionRepository;
    protected ExecutionEventProcessing executionEventProcessing;

    public CourseExecutionEventHandler(CourseExecutionRepository courseExecutionRepository, ExecutionEventProcessing executionEventProcessing) {
        this.courseExecutionRepository = courseExecutionRepository;
        this.executionEventProcessing = executionEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return courseExecutionRepository.findAllAggregateIds();
    }

}
