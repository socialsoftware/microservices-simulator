package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.events.publish.TeacherDeletedEvent;

public class CourseSubscribesTeacherDeleted extends EventSubscription {
    public CourseSubscribesTeacherDeleted(Course course) {
        super(course.getAggregateId(), 0, TeacherDeletedEvent.class);
    }
}
