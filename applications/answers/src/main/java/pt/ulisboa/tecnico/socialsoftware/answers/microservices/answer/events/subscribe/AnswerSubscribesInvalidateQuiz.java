package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.InvalidateQuizEvent;

public class AnswerSubscribesInvalidateQuiz extends EventSubscription {
    private final Answer answer;

    public AnswerSubscribesInvalidateQuiz(Answer answer) {
        super(answer.getQuiz().getQuizAggregateId(),
                answer.getQuiz().getQuizVersion(),
                InvalidateQuizEvent.class.getSimpleName());
        this.answer = answer;
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((InvalidateQuizEvent)event).getPublisherAggregateId() == answer.getQuiz().quizAggregateId;
    }
}
