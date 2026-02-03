package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuizEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.handling.handlers.ExecutionDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.handling.handlers.TopicUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.handling.handlers.TopicDeletedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.TopicDeletedEvent;

@Component
public class QuizEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private QuizEventProcessing quizEventProcessing;
    @Autowired
    private QuizRepository quizRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleExecutionDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(ExecutionDeletedEvent.class,
                new ExecutionDeletedEventHandler(quizRepository, quizEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleTopicUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(TopicUpdatedEvent.class,
                new TopicUpdatedEventHandler(quizRepository, quizEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleTopicDeletedEventEvents() {
        eventApplicationService.handleSubscribedEvent(TopicDeletedEvent.class,
                new TopicDeletedEventHandler(quizRepository, quizEventProcessing));
    }

}