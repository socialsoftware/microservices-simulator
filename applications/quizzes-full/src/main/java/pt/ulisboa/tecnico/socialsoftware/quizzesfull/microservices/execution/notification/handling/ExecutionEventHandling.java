package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.handling.handlers.ExecutionEventHandler;

@Component
public class ExecutionEventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private ExecutionEventHandler executionEventHandler;

    /*
        USER_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteUserEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class, executionEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateStudentNameEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class, executionEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleAnonymizeStudentEvents() {
        eventApplicationService.handleSubscribedEvent(AnonymizeStudentEvent.class, executionEventHandler);
    }
}
