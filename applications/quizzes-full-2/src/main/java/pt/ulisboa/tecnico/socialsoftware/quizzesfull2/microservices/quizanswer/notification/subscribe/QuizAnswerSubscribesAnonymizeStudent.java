package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerStudent;

public class QuizAnswerSubscribesAnonymizeStudent extends EventSubscription {
    public QuizAnswerSubscribesAnonymizeStudent(QuizAnswerStudent student) {
        super(student.getUserAggregateId(), student.getUserVersion(),
                AnonymizeStudentEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesAnonymizeStudent() {
    }
}
