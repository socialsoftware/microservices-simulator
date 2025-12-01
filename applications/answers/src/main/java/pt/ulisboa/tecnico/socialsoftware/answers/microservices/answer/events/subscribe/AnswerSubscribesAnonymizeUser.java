package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.AnonymizeUserEvent;

public class AnswerSubscribesAnonymizeUser extends EventSubscription {
    private final Answer answer;

    public AnswerSubscribesAnonymizeUser(Answer answer) {
        super(answer.getExecution().getExecutionAggregateId(),
                answer.getExecution().getExecutionVersion(),
                AnonymizeUserEvent.class.getSimpleName());
        this.answer = answer;
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((AnonymizeUserEvent)event).getStudentAggregateId() == answer.getUser().userAggregateId;
    }
}
