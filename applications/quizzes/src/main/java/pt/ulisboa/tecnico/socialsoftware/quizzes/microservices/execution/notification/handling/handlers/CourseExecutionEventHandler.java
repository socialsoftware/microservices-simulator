package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing;

public abstract class CourseExecutionEventHandler extends EventHandler {
    protected ExecutionEventProcessing executionEventProcessing;

    public CourseExecutionEventHandler(CourseExecutionRepository courseExecutionRepository, ExecutionEventProcessing executionEventProcessing) {
        super(courseExecutionRepository);
        this.executionEventProcessing = executionEventProcessing;
    }

}
