package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.eventProcessing.AnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.ExecutionUserUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.QuestionUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuestionUpdatedEvent;

@Component
public class AnswerEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private AnswerEventProcessing answerEventProcessing;
    @Autowired
    private AnswerRepository answerRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionUserUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionUserUpdatedEvent.class,
                new ExecutionUserUpdatedEventHandler(answerRepository, answerEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleQuestionUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(QuestionUpdatedEvent.class,
                new QuestionUpdatedEventHandler(answerRepository, answerEventProcessing));
    }

}