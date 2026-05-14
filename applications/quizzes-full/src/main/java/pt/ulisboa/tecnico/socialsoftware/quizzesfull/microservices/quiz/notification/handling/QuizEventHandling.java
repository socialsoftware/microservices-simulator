package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.handling.handlers.QuizEventHandler;

@Component
public class QuizEventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private QuizEventHandler quizEventHandler;

    /*
        QUESTION_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleUpdateQuestionEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateQuestionEvent.class, quizEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteQuestionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteQuestionEvent.class, quizEventHandler);
    }

    /*
        COURSE_EXECUTION_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteCourseExecutionEvent.class, quizEventHandler);
    }
}
