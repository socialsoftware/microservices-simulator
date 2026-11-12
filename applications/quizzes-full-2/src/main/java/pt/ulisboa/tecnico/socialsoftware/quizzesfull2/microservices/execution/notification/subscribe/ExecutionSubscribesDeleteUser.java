package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionStudent;

public class ExecutionSubscribesDeleteUser extends EventSubscription {
    public ExecutionSubscribesDeleteUser(ExecutionStudent student) {
        super(student.getUserAggregateId(), student.getUserVersion(),
                DeleteUserEvent.class.getSimpleName());
    }

    public ExecutionSubscribesDeleteUser() {
    }
}
