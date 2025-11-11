package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.ExecutionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers.DeleteUserEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.executionuser.events.publish.DeleteUserEvent;

@Component
public class ExecutionEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private ExecutionEventProcessing executionEventProcessing;
    @Autowired
    private ExecutionRepository executionRepository;

    /*
        DeleteUserEvent
     */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteUserEventEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class,
                new DeleteUserEventHandler(executionRepository, executionEventProcessing));
    }

}