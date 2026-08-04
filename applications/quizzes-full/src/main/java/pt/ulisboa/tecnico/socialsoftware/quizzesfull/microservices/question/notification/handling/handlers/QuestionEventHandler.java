package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.notification.handling.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.eventProcessing.QuestionEventProcessing;

@Component
public class QuestionEventHandler extends EventHandler {

    @Autowired
    private QuestionEventProcessing questionEventProcessing;

    @Autowired
    public QuestionEventHandler(QuestionRepository repository) {
        super(repository);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        if (event instanceof UpdateTopicEvent) {
            questionEventProcessing.processUpdateTopicEvent(subscriberAggregateId, (UpdateTopicEvent) event);
        } else if (event instanceof DeleteTopicEvent) {
            questionEventProcessing.processDeleteTopicEvent(subscriberAggregateId, (DeleteTopicEvent) event);
        }
    }
}
