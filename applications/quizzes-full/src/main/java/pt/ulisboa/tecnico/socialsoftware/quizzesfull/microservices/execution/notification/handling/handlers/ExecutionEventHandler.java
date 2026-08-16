package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.handling.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing;

@Component
public class ExecutionEventHandler extends EventHandler {
    @Autowired
    private ExecutionEventProcessing executionEventProcessing;

    @Autowired
    public ExecutionEventHandler(ExecutionRepository repository) {
        super(repository);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof DeleteUserEvent) {
            executionEventProcessing.processDeleteUserEvent(subscriberAggregateId, (DeleteUserEvent) event);
        } else if (event instanceof UpdateStudentNameEvent) {
            executionEventProcessing.processUpdateStudentNameEvent(subscriberAggregateId, (UpdateStudentNameEvent) event);
        } else if (event instanceof AnonymizeStudentEvent) {
            executionEventProcessing.processAnonymizeStudentEvent(subscriberAggregateId, (AnonymizeStudentEvent) event);
        }
    }
}
