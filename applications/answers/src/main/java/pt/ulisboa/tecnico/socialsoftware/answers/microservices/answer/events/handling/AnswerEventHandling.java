package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.AnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.UserDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.ExecutionDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.ExecutionUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.ExecutionUpdatedEvent;

@Component
public class AnswerEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private AnswerEventProcessing answerEventProcessing;
    @Autowired
    private AnswerRepository answerRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleUserDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(UserDeletedEvent.class,
                new UserDeletedEventHandler(answerRepository, answerEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionDeletedEvent.class,
                new ExecutionDeletedEventHandler(answerRepository, answerEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionUpdatedEvent.class,
                new ExecutionUpdatedEventHandler(answerRepository, answerEventProcessing));
    }

}