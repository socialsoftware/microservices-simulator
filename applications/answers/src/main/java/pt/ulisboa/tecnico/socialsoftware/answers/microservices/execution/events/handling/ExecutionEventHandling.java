package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.ExecutionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers.UserUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers.UserDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserDeletedEvent;

@Component
public class ExecutionEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private ExecutionEventProcessing executionEventProcessing;
    @Autowired
    private ExecutionRepository executionRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleUserUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(UserUpdatedEvent.class,
                new UserUpdatedEventHandler(executionRepository, executionEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUserDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(UserDeletedEvent.class,
                new UserDeletedEventHandler(executionRepository, executionEventProcessing));
    }

}