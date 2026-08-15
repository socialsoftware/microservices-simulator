package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic;

public class QuestionSubscribesDeleteTopic extends EventSubscription {
    public QuestionSubscribesDeleteTopic(QuestionTopic topic) {
        super(topic.getTopicAggregateId(), topic.getTopicVersion(), DeleteTopicEvent.class.getSimpleName());
    }

    public QuestionSubscribesDeleteTopic() {}
}
