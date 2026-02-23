package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers.UserDeletedEventHandler;

public class AnswerSubscribesUserDeleted extends EventSubscription {
    public AnswerSubscribesUserDeleted(Answer answer) {
        super(answer,
                UserDeletedEvent.class,
                UserDeletedEventHandler.class);
    }
}
