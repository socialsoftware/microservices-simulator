package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.QuestionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.handling.handlers.DeleteTopicEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.handling.handlers.UpdateTopicEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.events.publish.UpdateTopicEvent;

@Component
public class QuestionEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private QuestionEventProcessing questionEventProcessing;
    @Autowired
    private QuestionRepository questionRepository;

    /*
        TOPICS_EXISTS
     */
    @Scheduled(fixedDelay = 1000)
    public void handleUpdateTopicEvents() throws Throwable {
        eventApplicationService.handleSubscribedEvent(UpdateTopicEvent.class,
                new UpdateTopicEventHandler(questionRepository, questionEventProcessing));
    }

    /*
        TOPICS_EXISTS
     */
    @Scheduled(fixedDelay = 1000)
    public void handleDeleteTopicEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteTopicEvent.class,
                new DeleteTopicEventHandler(questionRepository, questionEventProcessing));
    }

    /*
        COURSE_EXISTS
     */
}
