package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish.ExecutionDeletedEvent;

public class AnswerSubscribesExecutionDeleted extends EventSubscription {
    

    public AnswerSubscribesExecutionDeleted( ) {
        super(.getQuiz().getQuizAggregateId(),
                .getQuiz().getQuizVersion(),
                ExecutionDeletedEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
