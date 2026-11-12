package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionStudent;

public class ExecutionSubscribesUpdateStudentName extends EventSubscription {
    public ExecutionSubscribesUpdateStudentName(ExecutionStudent student) {
        super(student.getUserAggregateId(), student.getUserVersion(),
                UpdateStudentNameEvent.class.getSimpleName());
    }

    public ExecutionSubscribesUpdateStudentName() {
    }
}
