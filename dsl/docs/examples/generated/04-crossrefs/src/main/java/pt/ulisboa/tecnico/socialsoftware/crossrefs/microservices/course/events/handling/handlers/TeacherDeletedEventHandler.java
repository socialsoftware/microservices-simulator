package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.CourseRepository;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.coordination.eventProcessing.CourseEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.TeacherDeletedEvent;

public class TeacherDeletedEventHandler extends CourseEventHandler {
    public TeacherDeletedEventHandler(CourseRepository courseRepository, CourseEventProcessing courseEventProcessing) {
        super(courseRepository, courseEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.courseEventProcessing.processTeacherDeletedEvent(subscriberAggregateId, (TeacherDeletedEvent) event);
    }
}
