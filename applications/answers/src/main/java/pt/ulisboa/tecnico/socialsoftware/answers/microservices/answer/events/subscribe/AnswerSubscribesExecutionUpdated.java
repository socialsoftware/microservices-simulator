package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.ExecutionUpdatedEvent;

public class AnswerSubscribesExecutionUpdated extends EventSubscription {
    

    public AnswerSubscribesExecutionUpdated( ) {
        super(.getQuiz().getQuizAggregateId(),
                .getQuiz().getQuizVersion(),
                ExecutionUpdatedEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
