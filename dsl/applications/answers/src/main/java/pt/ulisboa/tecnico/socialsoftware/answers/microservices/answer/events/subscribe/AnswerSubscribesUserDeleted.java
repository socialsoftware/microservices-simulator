package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserDeletedEvent;

public class AnswerSubscribesUserDeleted extends EventSubscription {
    public AnswerSubscribesUserDeleted(Answer answer) {
        super(answer.getAggregateId(), 0, UserDeletedEvent.class);
    }
}
