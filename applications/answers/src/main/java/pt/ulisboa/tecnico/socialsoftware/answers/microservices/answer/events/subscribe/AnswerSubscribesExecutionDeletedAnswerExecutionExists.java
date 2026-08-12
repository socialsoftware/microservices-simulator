package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionDeletedEvent;


public class AnswerSubscribesExecutionDeletedAnswerExecutionExists extends EventSubscription {
    public AnswerSubscribesExecutionDeletedAnswerExecutionExists(AnswerExecution execution) {
        super(execution.getExecutionAggregateId(),
                execution.getExecutionVersion(),
                ExecutionDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
