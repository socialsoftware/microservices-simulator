package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.CourseDeletedEvent;

public class EnrollmentSubscribesCourseDeleted extends EventSubscription {
    public EnrollmentSubscribesCourseDeleted(Enrollment enrollment) {
        super(enrollment.getAggregateId(), 0, CourseDeletedEvent.class.getSimpleName());
    }
}
