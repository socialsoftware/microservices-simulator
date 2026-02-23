package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUpdatedEvent;

public class QuizSubscribesExecutionUpdated extends EventSubscription {
    

    public QuizSubscribesExecutionUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
