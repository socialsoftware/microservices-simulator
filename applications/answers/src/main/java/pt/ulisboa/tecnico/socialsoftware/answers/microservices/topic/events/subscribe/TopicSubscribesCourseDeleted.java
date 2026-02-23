package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.handling.handlers.CourseDeletedEventHandler;

public class TopicSubscribesCourseDeleted extends EventSubscription {
    public TopicSubscribesCourseDeleted(Topic topic) {
        super(topic,
                CourseDeletedEvent.class,
                CourseDeletedEventHandler.class);
    }
}
