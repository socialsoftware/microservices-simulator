package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UpdateTopicEvent;

public class QuestionSubscribesUpdateTopic extends EventSubscription {
    public QuestionSubscribesUpdateTopic(QuestionTopic questiontopic) {
        super(questiontopic.getStudentAggregateId(),
                questiontopic.getStudentVersion(),
                UpdateTopicEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((UpdateTopicEvent)event).getGetPublisherAggregateId()() == questionTopic.getTopicAggregateId();
    }
}
