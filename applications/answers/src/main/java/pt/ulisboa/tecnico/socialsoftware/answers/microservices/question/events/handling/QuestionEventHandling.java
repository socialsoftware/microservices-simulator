package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.eventProcessing.QuestionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.handling.handlers.TopicUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicUpdatedEvent;

@Component
public class QuestionEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private QuestionEventProcessing questionEventProcessing;
    @Autowired
    private QuestionRepository questionRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleTopicUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(TopicUpdatedEvent.class,
                new TopicUpdatedEventHandler(questionRepository, questionEventProcessing));
    }

}