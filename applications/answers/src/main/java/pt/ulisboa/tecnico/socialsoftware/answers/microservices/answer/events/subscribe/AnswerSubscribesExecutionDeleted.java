package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionDeletedEvent;

public class AnswerSubscribesExecutionDeleted extends EventSubscription {
    public AnswerSubscribesExecutionDeleted(Answer answer) {
        super(answer.getAggregateId(), 0, ExecutionDeletedEvent.class.getSimpleName());
    }
}
