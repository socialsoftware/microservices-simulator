package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.DisenrollStudentFromExecutionEvent;

public class AnswerSubscribesDisenrollStudentFromExecution extends EventSubscription {
    private final Answer answer;

    public AnswerSubscribesDisenrollStudentFromExecution(Answer answer) {
        super(answer.getExecution().getExecutionAggregateId(),
                answer.getExecution().getExecutionVersion(),
                DisenrollStudentFromExecutionEvent.class.getSimpleName());
        this.answer = answer;
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((DisenrollStudentFromExecutionEvent)event).getStudentAggregateId() == answer.getUser().getUserAggregateId();
    }
}
