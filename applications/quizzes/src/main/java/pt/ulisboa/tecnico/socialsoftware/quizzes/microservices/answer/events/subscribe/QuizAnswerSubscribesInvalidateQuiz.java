package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.events.publish.InvalidateQuizEvent;

public class QuizAnswerSubscribesInvalidateQuiz extends EventSubscription {
    public QuizAnswerSubscribesInvalidateQuiz(QuizAnswer quizAnswer) {
        super(quizAnswer.getQuiz().getQuizAggregateId(),
                quizAnswer.getQuiz().getQuizVersion(),
                InvalidateQuizEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }

}