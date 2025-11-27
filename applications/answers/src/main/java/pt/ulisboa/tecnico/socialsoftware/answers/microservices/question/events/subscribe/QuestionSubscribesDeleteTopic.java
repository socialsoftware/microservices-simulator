package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.DeleteTopicEvent;

public class QuestionSubscribesDeleteTopic extends EventSubscription {
    public QuestionSubscribesDeleteTopic(QuestionTopic questiontopic) {
        super(questiontopic.getStudentAggregateId(),
                questiontopic.getStudentVersion(),
                DeleteTopicEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((DeleteTopicEvent)event).getGetPublisherAggregateId()() == questionTopic.getTopicAggregateId();
    }
}
