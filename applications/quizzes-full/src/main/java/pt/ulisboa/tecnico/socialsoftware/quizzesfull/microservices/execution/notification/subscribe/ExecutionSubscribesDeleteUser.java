package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionStudent;

public class ExecutionSubscribesDeleteUser extends EventSubscription {
    public ExecutionSubscribesDeleteUser(ExecutionStudent student) {
        super(student.getUserAggregateId(), 0L, DeleteUserEvent.class.getSimpleName());
    }

    public ExecutionSubscribesDeleteUser() {}
}
