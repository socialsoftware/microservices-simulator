package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.notification.handling.handlers.DeleteUserEventHandler;

@Component
public class CourseExecutionEventHandling implements EventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private ExecutionEventProcessing executionEventProcessing;
    @Autowired
    private CourseExecutionRepository courseExecutionRepository;

    /*
     * USER_EXISTS
     */
    @Scheduled(fixedDelay = 1000)
    public void handleRemoveUserEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class,
                new DeleteUserEventHandler(courseExecutionRepository, executionEventProcessing));
    }

}
