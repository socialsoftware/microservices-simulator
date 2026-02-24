package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities;

@Service
public class ExecutionEventProcessing {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ExecutionEventProcessing.class);

    @Autowired
    private ExecutionFunctionalities executionFunctionalities;

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent deleteUserEvent) {
        logger.info("Processing DeleteUserEvent: aggregateId={}, event={}", aggregateId, deleteUserEvent);
        executionFunctionalities.removeUserFromCourseExecution(aggregateId, deleteUserEvent.getPublisherAggregateId());
    }

}
