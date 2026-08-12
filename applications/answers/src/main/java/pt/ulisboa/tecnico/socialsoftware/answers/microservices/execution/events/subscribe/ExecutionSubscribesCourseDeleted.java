package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.events.CourseDeletedEvent;

public class ExecutionSubscribesCourseDeleted extends EventSubscription {
    public ExecutionSubscribesCourseDeleted(Execution execution) {
        super(execution.getAggregateId(), 0, CourseDeletedEvent.class.getSimpleName());
    }
}
