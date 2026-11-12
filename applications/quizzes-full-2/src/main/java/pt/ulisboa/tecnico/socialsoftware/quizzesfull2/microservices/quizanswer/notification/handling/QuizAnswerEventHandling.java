package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.handling.handlers.QuizAnswerEventHandler;

@Component
public class QuizAnswerEventHandling implements EventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private QuizAnswerEventHandler quizAnswerEventHandler;

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateStudentNameEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class, quizAnswerEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleAnonymizeStudentEvents() {
        eventApplicationService.handleSubscribedEvent(AnonymizeStudentEvent.class, quizAnswerEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteUserEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class, quizAnswerEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateQuestionEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateQuestionEvent.class, quizAnswerEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteCourseExecutionEvent.class, quizAnswerEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDisenrollStudentFromCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DisenrollStudentFromCourseExecutionEvent.class,
                quizAnswerEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleInvalidateQuizEvents() {
        eventApplicationService.handleSubscribedEvent(InvalidateQuizEvent.class, quizAnswerEventHandler);
    }
}
