package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.eventProcessing.TopicEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.events.CourseDeletedEvent;

public class CourseDeletedEventHandler extends TopicEventHandler {
    public CourseDeletedEventHandler(TopicRepository topicRepository, TopicEventProcessing topicEventProcessing) {
        super(topicRepository, topicEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.topicEventProcessing.processCourseDeletedEvent(subscriberAggregateId, (CourseDeletedEvent) event);
    }
}
