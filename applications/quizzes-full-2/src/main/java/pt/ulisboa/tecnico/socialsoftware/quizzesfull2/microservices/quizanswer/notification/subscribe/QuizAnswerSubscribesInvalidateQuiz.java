package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerQuiz;

public class QuizAnswerSubscribesInvalidateQuiz extends EventSubscription {
    public QuizAnswerSubscribesInvalidateQuiz(QuizAnswerQuiz quiz) {
        super(quiz.getQuizAggregateId(), quiz.getQuizVersion(),
                InvalidateQuizEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesInvalidateQuiz() {
    }
}
