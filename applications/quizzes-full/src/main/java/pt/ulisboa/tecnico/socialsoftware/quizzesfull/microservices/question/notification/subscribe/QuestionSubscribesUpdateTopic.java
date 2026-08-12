package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic;

public class QuestionSubscribesUpdateTopic extends EventSubscription {
    public QuestionSubscribesUpdateTopic(QuestionTopic topic) {
        super(topic.getTopicAggregateId(), topic.getTopicVersion(), UpdateTopicEvent.class.getSimpleName());
    }

    public QuestionSubscribesUpdateTopic() {}
}
