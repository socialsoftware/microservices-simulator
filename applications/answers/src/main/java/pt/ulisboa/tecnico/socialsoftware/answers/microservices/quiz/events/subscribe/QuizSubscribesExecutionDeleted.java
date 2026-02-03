package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.ExecutionDeletedEvent;

public class QuizSubscribesExecutionDeleted extends EventSubscription {
    

    public QuizSubscribesExecutionDeleted( ) {
        super(.getAggregateId(),
                0,
                ExecutionDeletedEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
