package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.eventProcessing.EventProcessingHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.AGGREGATE_DELETED;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.repository.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CourseDeletedEventHandler implements EventProcessingHandler<CourseDeletedEvent, Execution> {

    private final ExecutionRepository executionRepository;

    public CourseDeletedEventHandler(ExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @Override
    public void handleEvent(Execution execution, CourseDeletedEvent event) {
        // Reference constraint: prevent deletion if references exist
        if (execution.getCourse() != null) {
            Integer referencedCourseId = execution.getCourse().getCourseAggregateId();
            if (referencedCourseId != null && referencedCourseId.equals(event.getPublisherAggregateId())) {
                throw new SimulatorException(AGGREGATE_DELETED, "Cannot delete course that has executions");
            }
        }
    }
}
