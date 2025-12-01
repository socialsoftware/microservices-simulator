package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.DeleteUserEvent;

public class ExecutionSubscribesDeleteUser extends EventSubscription {
    private final ExecutionUser executionuser;

    public ExecutionSubscribesDeleteUser(ExecutionUser executionuser) {
        super(executionuser.getCourse().getCourseAggregateId(),
                executionuser.getCourse().getCourseVersion(),
                DeleteUserEvent.class.getSimpleName());
        this.executionuser = executionuser;
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((DeleteUserEvent)event).getGetUserAggregateId()() == executionUser.getStudentAggregateId();
    }
}
