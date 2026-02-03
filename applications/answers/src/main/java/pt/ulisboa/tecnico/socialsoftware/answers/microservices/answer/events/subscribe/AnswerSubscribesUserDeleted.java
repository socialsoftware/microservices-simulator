package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.UserDeletedEvent;

public class AnswerSubscribesUserDeleted extends EventSubscription {
    

    public AnswerSubscribesUserDeleted( ) {
        super(.getQuiz().getQuizAggregateId(),
                .getQuiz().getQuizVersion(),
                UserDeletedEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
