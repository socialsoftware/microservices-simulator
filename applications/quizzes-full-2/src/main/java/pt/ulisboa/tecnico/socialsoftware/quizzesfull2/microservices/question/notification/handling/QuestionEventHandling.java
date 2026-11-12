package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.notification.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.notification.handling.handlers.QuestionEventHandler;

@Component
public class QuestionEventHandling implements EventHandling {

    @Autowired
    private EventApplicationService eventApplicationService;

    @Autowired
    private QuestionEventHandler questionEventHandler;

    @Scheduled(fixedDelay = 1000)
    public void handleUpdateTopicEvents() {
        eventApplicationService.handleSubscribedEvent(UpdateTopicEvent.class, questionEventHandler);
    }

    @Scheduled(fixedDelay = 1000)
    public void handleDeleteTopicEvents() {
        eventApplicationService.handleSubscribedEvent(DeleteTopicEvent.class, questionEventHandler);
    }
}
