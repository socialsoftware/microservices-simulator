package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.AnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.AnonymizeUserEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.AnonymizeUserEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.UpdateStudentNameEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.DisenrollStudentFromExecutionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.DisenrollStudentFromExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.DeleteExecutionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.DeleteExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.InvalidateQuizEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.DeleteQuestionEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.DeleteQuestionEvent;

@Component
public class AnswerEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private AnswerEventProcessing answerEventProcessing;
    @Autowired
    private AnswerRepository answerRepository;

    /*
        AnonymizeUserEvent
     */
    @Scheduled(fixedDelay = 1000)
    public void handleAnonymizeUserEventEvents() {
        eventApplicationService.handleSubscribedEvent(AnonymizeUserEvent.class,
                new AnonymizeUserEventHandler(answerRepository, answerEventProcessing));
    }

    /*
        UpdateStudentNameEvent
     */
    @Scheduled(fixedDelay = 1000)
    public void handleUpdateStudentNameEventEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateStudentNameEvent.class,
                new UpdateStudentNameEventHandler(answerRepository, answerEventProcessing));
    }

    /*
        DisenrollStudentFromExecutionEvent
     */
    @Scheduled(fixedDelay = 1000)
    public void handleDisenrollStudentFromExecutionEventEvents() {
        eventApplicationService.handleSubscribedEvent(DisenrollStudentFromExecutionEvent.class,
                new DisenrollStudentFromExecutionEventHandler(answerRepository, answerEventProcessing));
    }

    /*
        DeleteExecutionEvent
     */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteExecutionEventEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteExecutionEvent.class,
                new DeleteExecutionEventHandler(answerRepository, answerEventProcessing));
    }

    /*
        InvalidateQuizEvent
     */
    @Scheduled(fixedDelay = 1000)
    public void handleInvalidateQuizEventEvents() {
        eventApplicationService.handleSubscribedEvent(InvalidateQuizEvent.class,
                new InvalidateQuizEventHandler(answerRepository, answerEventProcessing));
    }

    /*
        DeleteQuestionEvent
     */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteQuestionEventEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteQuestionEvent.class,
                new DeleteQuestionEventHandler(answerRepository, answerEventProcessing));
    }

}