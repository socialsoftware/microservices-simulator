package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionStudent;

public class ExecutionSubscribesAnonymizeStudent extends EventSubscription {
    public ExecutionSubscribesAnonymizeStudent(ExecutionStudent student) {
        super(student.getUserAggregateId(), student.getUserVersion(),
                AnonymizeStudentEvent.class.getSimpleName());
    }

    public ExecutionSubscribesAnonymizeStudent() {
    }
}
