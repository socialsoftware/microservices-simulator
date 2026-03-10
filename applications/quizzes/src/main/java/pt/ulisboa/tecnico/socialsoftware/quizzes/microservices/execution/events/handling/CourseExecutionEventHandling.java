package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.CreateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling.handlers.CreateQuestionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling.handlers.DeleteQuestionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling.handlers.DeleteUserEventHandler;

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

    @Scheduled(fixedDelay = 1000)
    public void handleCreateQuestionEvents() {
        eventApplicationService.handleSubscribedEvent(CreateQuestionEvent.class,
                new CreateQuestionEventHandler(courseExecutionRepository, executionEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteQuestionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteQuestionEvent.class,
                new DeleteQuestionEventHandler(courseExecutionRepository, executionEventProcessing));
    }
}
