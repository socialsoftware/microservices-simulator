package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;

public class AnswerSubscribesQuizDeleted extends EventSubscription {
    public AnswerSubscribesQuizDeleted(Answer answer) {
        super(answer.getAggregateId(), 0, QuizDeletedEvent.class);
    }
}
