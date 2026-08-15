package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerStudent;

public class QuizAnswerSubscribesDeleteUser extends EventSubscription {
    public QuizAnswerSubscribesDeleteUser(QuizAnswerStudent student) {
        super(student.getUserAggregateId(), student.getUserVersion(),
                DeleteUserEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesDeleteUser() {
    }
}
