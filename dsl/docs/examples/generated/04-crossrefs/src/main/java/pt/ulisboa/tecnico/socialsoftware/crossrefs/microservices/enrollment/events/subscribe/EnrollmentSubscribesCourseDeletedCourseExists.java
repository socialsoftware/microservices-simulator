package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentCourse;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.publish.CourseDeletedEvent;


public class EnrollmentSubscribesCourseDeletedCourseExists extends EventSubscription {
    public EnrollmentSubscribesCourseDeletedCourseExists(EnrollmentCourse course) {
        super(course.getCourseAggregateId(),
                course.getCourseVersion(),
                CourseDeletedEvent.class);
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
