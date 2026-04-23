package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.CourseTeacher;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.TeacherDeletedEvent;


public class CourseSubscribesTeacherDeletedTeacherRef extends EventSubscription {
    public CourseSubscribesTeacherDeletedTeacherRef(CourseTeacher teacher) {
        super(teacher.getTeacherAggregateId(),
                teacher.getTeacherVersion(),
                TeacherDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
