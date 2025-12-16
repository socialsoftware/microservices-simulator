package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.DeleteExecutionEvent;

public class AnswerSubscribesDeleteExecution extends EventSubscription {
    

    public AnswerSubscribesDeleteExecution(Answer answer) {
        super(answer.getExecution().getExecutionAggregateId(),
                answer.getExecution().getExecutionVersion(),
                DeleteExecutionEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
