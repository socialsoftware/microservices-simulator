package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.ActivateUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.handling.handlers.ExecutionEventHandler;

@Component
public class ExecutionEventHandling implements EventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private ExecutionEventHandler executionEventHandler;

    @Scheduled(fixedDelay = 1000)
    public void handleActivateUserEvents() {
        eventApplicationService.handleSubscribedEvent(ActivateUserEvent.class, executionEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateStudentNameEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class, executionEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleAnonymizeStudentEvents() {
        eventApplicationService.handleSubscribedEvent(AnonymizeStudentEvent.class, executionEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteUserEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class, executionEventHandler);
    }
}
