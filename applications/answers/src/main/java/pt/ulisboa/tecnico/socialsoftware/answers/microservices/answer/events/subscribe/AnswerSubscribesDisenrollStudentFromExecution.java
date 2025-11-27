package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.DisenrollStudentFromExecutionEvent;

public class AnswerSubscribesDisenrollStudentFromExecution extends EventSubscription {
    public AnswerSubscribesDisenrollStudentFromExecution(AnswerExecution answerexecution) {
        super(answerexecution.getStudentAggregateId(),
                answerexecution.getStudentVersion(),
                DisenrollStudentFromExecutionEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((DisenrollStudentFromExecutionEvent)event).getGetExecutionAggregateId()() == answerExecution.getExecutionAggregateId();
    }
}
