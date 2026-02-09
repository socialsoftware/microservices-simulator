package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;


public class ExecutionSubscribesCourseDeletedCourseExists extends EventSubscription {
    public ExecutionSubscribesCourseDeletedCourseExists(ExecutionCourse course) {
        super(course.getCourseAggregateId(),
                course.getCourseVersion(),
                CourseDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
