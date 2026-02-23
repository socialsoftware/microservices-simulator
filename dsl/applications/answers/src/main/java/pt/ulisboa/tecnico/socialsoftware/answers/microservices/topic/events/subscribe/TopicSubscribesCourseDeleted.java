package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;

public class TopicSubscribesCourseDeleted extends EventSubscription {
    public TopicSubscribesCourseDeleted(Topic topic) {
        super(topic.getAggregateId(), 0, CourseDeletedEvent.class);
    }
}
