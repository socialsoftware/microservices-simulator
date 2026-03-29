package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.AnonymizeStudentEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.DeleteCourseExecutionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.DeleteUserEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.DisenrollStudentFromCourseExecutionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.InvalidateQuizEventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers.UpdateStudentNameEventHandler;

@Component
public class QuizAnswerEventHandling implements EventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private QuizAnswerRepository quizAnswerRepository;
    @Autowired
    private QuizAnswerEventProcessing quizAnswerEventProcessing;

    /*
        USER_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleRemoveUserEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteUserEvent.class,
                new DeleteUserEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

    /*
        COURSE_EXECUTION_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteCourseExecutionEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteCourseExecutionEvent.class,
                new DeleteCourseExecutionEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

    /*
        QUIZ_EXISTS
    */
    @Scheduled(fixedDelay = 1000)
    public void handleInvalidateQuizEvents() {
        eventApplicationService.handleSubscribedEvent(InvalidateQuizEvent.class,
                new InvalidateQuizEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

    /*
        USER_EXISTS (unenroll)
    */
    @Scheduled(fixedDelay = 1000)
    public void handleUnenrollStudentEvent() {
        eventApplicationService.handleSubscribedEvent(DisenrollStudentFromCourseExecutionEvent.class,
                new DisenrollStudentFromCourseExecutionEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

    /*
        USER_EXISTS (name update)
    */
    @Scheduled(fixedDelay = 1000)
    public void handleUpdateExecutionStudentNameEvent() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class,
                new UpdateStudentNameEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

    /*
        USER_EXISTS (anonymize)
    */
    @Scheduled(fixedDelay = 1000)
    public void handleAnonymizeStudentEvents() {
        eventApplicationService.handleSubscribedEvent(AnonymizeStudentEvent.class,
                new AnonymizeStudentEventHandler(quizAnswerRepository, quizAnswerEventProcessing));
    }

}
