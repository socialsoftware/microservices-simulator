package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionStudent;

public class ExecutionSubscribesAnonymizeStudent extends EventSubscription {
    public ExecutionSubscribesAnonymizeStudent(ExecutionStudent student) {
        super(student.getUserAggregateId(), 0L, AnonymizeStudentEvent.class.getSimpleName());
    }

    public ExecutionSubscribesAnonymizeStudent() {}
}
