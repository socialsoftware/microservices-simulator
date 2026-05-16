package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;

public class QuizAnswerSubscribesInvalidateQuiz extends EventSubscription {

    public QuizAnswerSubscribesInvalidateQuiz(QuizAnswer quizAnswer) {
        super(quizAnswer.getQuizAggregateId(), quizAnswer.getQuizVersion(), InvalidateQuizEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesInvalidateQuiz() {}
}
