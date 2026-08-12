package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUserUpdatedEvent;

public class AnswerSubscribesExecutionUserUpdated extends EventSubscription {
    

    public AnswerSubscribesExecutionUserUpdated(Answer answer) {
        super(answer.getQuiz().getQuizAggregateId(),
                answer.getQuiz().getQuizVersion(),
                ExecutionUserUpdatedEvent.class.getSimpleName());
        
    }

    public AnswerSubscribesExecutionUserUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
