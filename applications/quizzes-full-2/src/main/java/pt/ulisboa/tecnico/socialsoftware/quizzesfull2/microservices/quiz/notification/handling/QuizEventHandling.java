package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.handling.handlers.QuizEventHandler;

@Component
public class QuizEventHandling implements EventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private QuizEventHandler quizEventHandler;

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateQuestionEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateQuestionEvent.class, quizEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteQuestionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteQuestionEvent.class, quizEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteCourseExecutionEvent.class, quizEventHandler);
    }
}
