package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.handling.handlers.CourseDeletedEventHandler;

public class ExecutionSubscribesCourseDeleted extends EventSubscription {
    public ExecutionSubscribesCourseDeleted(Execution execution) {
        super(execution,
                CourseDeletedEvent.class,
                CourseDeletedEventHandler.class);
    }
}
