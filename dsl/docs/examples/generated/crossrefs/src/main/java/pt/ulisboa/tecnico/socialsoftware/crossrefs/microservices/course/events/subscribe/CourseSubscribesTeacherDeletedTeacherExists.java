package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.CourseTeacher;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.events.publish.TeacherDeletedEvent;


public class CourseSubscribesTeacherDeletedTeacherExists extends EventSubscription {
    public CourseSubscribesTeacherDeletedTeacherExists(CourseTeacher teacher) {
        super(teacher.getTeacherAggregateId(),
                teacher.getTeacherVersion(),
                TeacherDeletedEvent.class);
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
