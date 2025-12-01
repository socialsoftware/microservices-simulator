package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.DeleteTopicEvent;

public class QuestionSubscribesDeleteTopic extends EventSubscription {
    private final QuestionTopic questiontopic;

    public QuestionSubscribesDeleteTopic(QuestionTopic questiontopic) {
        super(questiontopic.getCourse().getCourseAggregateId(),
                questiontopic.getCourse().getCourseVersion(),
                DeleteTopicEvent.class.getSimpleName());
        this.questiontopic = questiontopic;
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((DeleteTopicEvent)event).getGetPublisherAggregateId()() == questionTopic.getTopicAggregateId();
    }
}
