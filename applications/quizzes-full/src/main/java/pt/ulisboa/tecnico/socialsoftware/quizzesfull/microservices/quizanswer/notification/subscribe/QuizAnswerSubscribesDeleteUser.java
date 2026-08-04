package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;

public class QuizAnswerSubscribesDeleteUser extends EventSubscription {

    public QuizAnswerSubscribesDeleteUser(QuizAnswer quizAnswer) {
        super(quizAnswer.getUserAggregateId(), quizAnswer.getUserVersion(), DeleteUserEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesDeleteUser() {}
}
