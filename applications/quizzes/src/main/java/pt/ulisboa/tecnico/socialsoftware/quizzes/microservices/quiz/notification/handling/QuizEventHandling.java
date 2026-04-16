package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.eventProcessing.QuizEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling.handlers.DeleteCourseExecutionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling.handlers.DeleteQuestionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling.handlers.UpdateQuestionEventHandler;

@Component
public class QuizEventHandling implements EventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private QuizEventProcessing quizEventProcessing;
    @Autowired
    private QuizRepository quizRepository;

    /*
        COURSE_EXECUTION_EXISTS
     */
    @Scheduled(fixedDelay = 1000)
    public void handleRemoveCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteCourseExecutionEvent.class,
                new DeleteCourseExecutionEventHandler(quizRepository, quizEventProcessing));
    }

    /*
        QUESTION_EXISTS
     */
    @Scheduled(fixedDelay = 1000)
    public void handleUpdateQuestionEvent() {
        eventApplicationService.handleSubscribedEvent(UpdateQuestionEvent.class,
                new UpdateQuestionEventHandler(quizRepository, quizEventProcessing));
    }

    /*
        QUESTION_EXISTS
     */
    @Scheduled(fixedDelay = 1000)
    public void handleRemoveQuestionEvent() {
        eventApplicationService.handleSubscribedEvent(DeleteQuestionEvent.class,
                new DeleteQuestionEventHandler(quizRepository, quizEventProcessing));
    }

}
