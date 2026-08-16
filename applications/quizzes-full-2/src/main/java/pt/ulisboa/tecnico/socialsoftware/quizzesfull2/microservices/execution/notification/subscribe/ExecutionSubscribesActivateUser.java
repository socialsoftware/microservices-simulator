package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.ActivateUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionStudent;

public class ExecutionSubscribesActivateUser extends EventSubscription {
    public ExecutionSubscribesActivateUser(ExecutionStudent student) {
        super(student.getUserAggregateId(), student.getUserVersion(),
                ActivateUserEvent.class.getSimpleName());
    }

    public ExecutionSubscribesActivateUser() {
    }
}
