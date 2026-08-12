package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuestionUpdatedEvent;

public class AnswerSubscribesQuestionUpdated extends EventSubscription {
    

    public AnswerSubscribesQuestionUpdated(AnswerQuestion answerQuestion) {
        super(answerQuestion.getQuestionAggregateId(),
                answerQuestion.getQuestionVersion(),
                QuestionUpdatedEvent.class.getSimpleName());
        
    }

    public AnswerSubscribesQuestionUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
