package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicDeletedEvent;


public class QuestionSubscribesTopicDeletedTopicsRef extends EventSubscription {
    public QuestionSubscribesTopicDeletedTopicsRef(QuestionTopic topics) {
        super(topics.getTopicAggregateId(),
                topics.getTopicVersion(),
                TopicDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
