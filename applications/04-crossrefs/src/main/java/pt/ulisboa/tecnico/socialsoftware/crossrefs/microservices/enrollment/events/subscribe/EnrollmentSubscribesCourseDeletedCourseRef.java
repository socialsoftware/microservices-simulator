package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentCourse;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.CourseDeletedEvent;


public class EnrollmentSubscribesCourseDeletedCourseRef extends EventSubscription {
    public EnrollmentSubscribesCourseDeletedCourseRef(EnrollmentCourse course) {
        super(course.getCourseAggregateId(),
                course.getCourseVersion(),
                CourseDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
