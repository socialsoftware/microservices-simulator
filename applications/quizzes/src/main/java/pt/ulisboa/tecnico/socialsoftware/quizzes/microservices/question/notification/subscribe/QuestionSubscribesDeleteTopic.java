package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionTopic;

public class QuestionSubscribesDeleteTopic extends EventSubscription {
    public QuestionSubscribesDeleteTopic(QuestionTopic questionTopic) {
        super(questionTopic.getTopicAggregateId(),
                questionTopic.getTopicVersion(),
                DeleteTopicEvent.class.getSimpleName());
    }

    public QuestionSubscribesDeleteTopic() {}

    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}