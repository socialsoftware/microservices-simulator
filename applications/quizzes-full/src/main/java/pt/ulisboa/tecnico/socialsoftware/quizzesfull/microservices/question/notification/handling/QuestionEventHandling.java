package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.notification.handling.handlers.QuestionEventHandler;

@Component
public class QuestionEventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private QuestionEventHandler questionEventHandler;

    /*
        TOPICS_EXIST
    */
    @Scheduled(fixedDelay = 1000)
    public void handleUpdateTopicEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateTopicEvent.class, questionEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteTopicEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteTopicEvent.class, questionEventHandler);
    }
}
