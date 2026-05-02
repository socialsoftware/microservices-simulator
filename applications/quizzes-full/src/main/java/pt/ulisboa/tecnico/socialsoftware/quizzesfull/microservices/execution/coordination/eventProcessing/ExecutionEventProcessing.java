package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.functionalities.ExecutionFunctionalities;

@Service
public class ExecutionEventProcessing {

    @Autowired
    private ExecutionFunctionalities executionFunctionalities;

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent event) {
        executionFunctionalities.removeStudentFromExecutionByEvent(aggregateId, event.getPublisherAggregateId());
    }

    public void processUpdateStudentNameEvent(Integer aggregateId, UpdateStudentNameEvent event) {
        executionFunctionalities.updateStudentNameByEvent(aggregateId, event.getStudentAggregateId(), event.getUpdatedName());
    }

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent event) {
        executionFunctionalities.anonymizeStudentByEvent(aggregateId, event.getStudentAggregateId());
    }
}
