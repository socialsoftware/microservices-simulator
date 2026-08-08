package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.handling.handlers;

import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.ActivateUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing;

@Component
public class ExecutionEventHandler extends EventHandler {

    private final ExecutionEventProcessing executionEventProcessing;

    public ExecutionEventHandler(ExecutionRepository executionRepository,
                                 ExecutionEventProcessing executionEventProcessing) {
        super(executionRepository);
        this.executionEventProcessing = executionEventProcessing;
    }

    @Override
    protected Class<? extends Aggregate> aggregateType() {
        return Execution.class;
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof ActivateUserEvent e) {
            executionEventProcessing.processActivateUserEvent(subscriberAggregateId, e);
        } else if (event instanceof UpdateStudentNameEvent e) {
            executionEventProcessing.processUpdateStudentNameEvent(subscriberAggregateId, e);
        } else if (event instanceof AnonymizeStudentEvent e) {
            executionEventProcessing.processAnonymizeStudentEvent(subscriberAggregateId, e);
        } else if (event instanceof DeleteUserEvent e) {
            executionEventProcessing.processDeleteUserEvent(subscriberAggregateId, e);
        }
    }
}
