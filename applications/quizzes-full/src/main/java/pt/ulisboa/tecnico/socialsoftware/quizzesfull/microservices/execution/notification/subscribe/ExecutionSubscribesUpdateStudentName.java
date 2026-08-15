package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionStudent;

public class ExecutionSubscribesUpdateStudentName extends EventSubscription {
    public ExecutionSubscribesUpdateStudentName(ExecutionStudent student) {
        super(student.getUserAggregateId(), 0L, UpdateStudentNameEvent.class.getSimpleName());
    }

    public ExecutionSubscribesUpdateStudentName() {}
}
