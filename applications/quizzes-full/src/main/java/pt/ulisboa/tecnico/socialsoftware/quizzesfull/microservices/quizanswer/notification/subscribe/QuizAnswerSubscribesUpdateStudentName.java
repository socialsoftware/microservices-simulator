package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;

public class QuizAnswerSubscribesUpdateStudentName extends EventSubscription {

    public QuizAnswerSubscribesUpdateStudentName(QuizAnswer quizAnswer) {
        super(quizAnswer.getUserAggregateId(), quizAnswer.getUserVersion(), UpdateStudentNameEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesUpdateStudentName() {}
}
