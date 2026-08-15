package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.handling.handlers.QuizAnswerEventHandler;

@Component
public class QuizAnswerEventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private QuizAnswerEventHandler quizAnswerEventHandler;

    /*
        USER_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteUserEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class, quizAnswerEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateStudentNameEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class, quizAnswerEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleAnonymizeStudentEvents() {
        eventApplicationService.handleSubscribedEvent(AnonymizeStudentEvent.class, quizAnswerEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDisenrollStudentFromCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DisenrollStudentFromCourseExecutionEvent.class, quizAnswerEventHandler);
    }

    /*
        QUESTION_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleUpdateQuestionEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateQuestionEvent.class, quizAnswerEventHandler);
    }

    /*
        COURSE_EXECUTION_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteCourseExecutionEvent.class, quizAnswerEventHandler);
    }

    /*
        QUIZ_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleInvalidateQuizEvents() {
        eventApplicationService.handleSubscribedEvent(InvalidateQuizEvent.class, quizAnswerEventHandler);
    }
}
