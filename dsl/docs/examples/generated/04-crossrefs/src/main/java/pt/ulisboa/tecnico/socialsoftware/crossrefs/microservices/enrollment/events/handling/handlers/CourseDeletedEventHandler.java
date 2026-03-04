package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentRepository;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.eventProcessing.EnrollmentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.CourseDeletedEvent;

public class CourseDeletedEventHandler extends EnrollmentEventHandler {
    public CourseDeletedEventHandler(EnrollmentRepository enrollmentRepository, EnrollmentEventProcessing enrollmentEventProcessing) {
        super(enrollmentRepository, enrollmentEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.enrollmentEventProcessing.processCourseDeletedEvent(subscriberAggregateId, (CourseDeletedEvent) event);
    }
}
