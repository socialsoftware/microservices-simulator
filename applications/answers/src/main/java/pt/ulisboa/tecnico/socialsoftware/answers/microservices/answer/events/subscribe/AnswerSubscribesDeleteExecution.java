package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.DeleteExecutionEvent;

public class AnswerSubscribesDeleteExecution extends EventSubscription {
    public AnswerSubscribesDeleteExecution(AnswerExecution answerexecution) {
        super(answerexecution.getStudentAggregateId(),
                answerexecution.getStudentVersion(),
                DeleteExecutionEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((DeleteExecutionEvent)event).getGetExecutionAggregateId()() == answerExecution.getExecutionAggregateId();
    }
}
