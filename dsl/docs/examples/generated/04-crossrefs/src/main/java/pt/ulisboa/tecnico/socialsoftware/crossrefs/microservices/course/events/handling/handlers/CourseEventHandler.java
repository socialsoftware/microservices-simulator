package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.coordination.eventProcessing.CourseEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.CourseRepository;

public abstract class CourseEventHandler extends EventHandler {
    private CourseRepository courseRepository;
    protected CourseEventProcessing courseEventProcessing;

    public CourseEventHandler(CourseRepository courseRepository, CourseEventProcessing courseEventProcessing) {
        this.courseRepository = courseRepository;
        this.courseEventProcessing = courseEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return courseRepository.findAll().stream().map(Course::getAggregateId).collect(Collectors.toSet());
    }

}
