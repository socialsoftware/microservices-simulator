package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.events.CourseDeletedEvent;


public class TopicSubscribesCourseDeletedCourseRef extends EventSubscription {
    public TopicSubscribesCourseDeletedCourseRef(TopicCourse course) {
        super(course.getCourseAggregateId(),
                course.getCourseVersion(),
                CourseDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
