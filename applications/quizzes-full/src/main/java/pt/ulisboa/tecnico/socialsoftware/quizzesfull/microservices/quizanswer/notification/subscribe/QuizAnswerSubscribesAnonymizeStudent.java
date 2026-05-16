package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;

public class QuizAnswerSubscribesAnonymizeStudent extends EventSubscription {

    public QuizAnswerSubscribesAnonymizeStudent(QuizAnswer quizAnswer) {
        super(quizAnswer.getUserAggregateId(), quizAnswer.getUserVersion(), AnonymizeStudentEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesAnonymizeStudent() {}
}
