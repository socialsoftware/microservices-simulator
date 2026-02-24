package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswer;

public class QuizAnswerSubscribesInvalidateQuiz extends EventSubscription {
    public QuizAnswerSubscribesInvalidateQuiz(QuizAnswer quizAnswer) {
        super(quizAnswer.getQuiz().getQuizAggregateId(),
                quizAnswer.getQuiz().getQuizVersion(),
                InvalidateQuizEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesInvalidateQuiz() {}

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }

}