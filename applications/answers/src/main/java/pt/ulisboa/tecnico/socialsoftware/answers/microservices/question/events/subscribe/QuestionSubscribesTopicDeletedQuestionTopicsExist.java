package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicDeletedEvent;


public class QuestionSubscribesTopicDeletedQuestionTopicsExist extends EventSubscription {
    public QuestionSubscribesTopicDeletedQuestionTopicsExist(QuestionTopic topics) {
        super(topics.getTopicAggregateId(),
                topics.getTopicVersion(),
                TopicDeletedEvent.class);
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
