package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.ActivateUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.functionalities.ExecutionFunctionalities;

@Service
public class ExecutionEventProcessing {

    @Autowired
    private ExecutionFunctionalities executionFunctionalities;

    public void processActivateUserEvent(Integer aggregateId, ActivateUserEvent event) {
        executionFunctionalities.setStudentActiveByEvent(aggregateId, event.getUserAggregateId(),
                event.isActive(), event.getPublisherAggregateVersion());
    }

    public void processUpdateStudentNameEvent(Integer aggregateId, UpdateStudentNameEvent event) {
        executionFunctionalities.setStudentNameByEvent(aggregateId, event.getStudentAggregateId(),
                event.getUpdatedName(), event.getPublisherAggregateVersion());
    }

    public void processAnonymizeStudentEvent(Integer aggregateId, AnonymizeStudentEvent event) {
        executionFunctionalities.anonymizeStudentByEvent(aggregateId, event.getStudentAggregateId(),
                event.getName(), event.getUsername(), event.getPublisherAggregateVersion());
    }

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent event) {
        executionFunctionalities.removeDeletedStudentByEvent(aggregateId, event.getUserAggregateId());
    }
}
