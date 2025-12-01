package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.UpdateTopicEvent;

public class QuestionSubscribesUpdateTopic extends EventSubscription {
    private final QuestionTopic questiontopic;

    public QuestionSubscribesUpdateTopic(QuestionTopic questiontopic) {
        super(questiontopic.getCourse().getCourseAggregateId(),
                questiontopic.getCourse().getCourseVersion(),
                UpdateTopicEvent.class.getSimpleName());
        this.questiontopic = questiontopic;
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((UpdateTopicEvent)event).getGetPublisherAggregateId()() == questionTopic.getTopicAggregateId();
    }
}
