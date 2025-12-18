package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.CourseExecutionFunctionalities;

@Service
public class CourseExecutionEventProcessing {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CourseExecutionEventProcessing.class);

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities;

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent deleteUserEvent) {
        logger.info("Processing DeleteUserEvent: aggregateId={}, event={}", aggregateId, deleteUserEvent);
        courseExecutionFunctionalities.removeUserFromCourseExecution(aggregateId, deleteUserEvent.getPublisherAggregateId());
    }

}
