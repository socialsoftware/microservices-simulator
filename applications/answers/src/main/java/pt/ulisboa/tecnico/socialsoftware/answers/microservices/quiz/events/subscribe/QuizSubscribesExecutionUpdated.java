package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUpdatedEvent;

public class QuizSubscribesExecutionUpdated extends EventSubscription {
    

    public QuizSubscribesExecutionUpdated(QuizExecution quizExecution) {
        super(quizExecution.getExecutionAggregateId(),
                quizExecution.getExecutionVersion(),
                ExecutionUpdatedEvent.class.getSimpleName());
        
    }

    public QuizSubscribesExecutionUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
