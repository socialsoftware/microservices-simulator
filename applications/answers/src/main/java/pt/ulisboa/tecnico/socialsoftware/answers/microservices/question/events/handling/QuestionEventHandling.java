package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuestionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.handling.handlers.DeleteTopicEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.questiontopic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.handling.handlers.UpdateTopicEventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.questiontopic.events.publish.UpdateTopicEvent;

@Component
public class QuestionEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private QuestionEventProcessing questionEventProcessing;
    @Autowired
    private QuestionRepository questionRepository;

    /*
        DeleteTopicEvent
     */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteTopicEventEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteTopicEvent.class,
                new DeleteTopicEventHandler(questionRepository, questionEventProcessing));
    }

    /*
        UpdateTopicEvent
     */
    @Scheduled(fixedDelay = 1000)
    public void handleUpdateTopicEventEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateTopicEvent.class,
                new UpdateTopicEventHandler(questionRepository, questionEventProcessing));
    }

}