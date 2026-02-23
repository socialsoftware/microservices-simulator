package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;

public class QuizSubscribesExecutionDeleted extends EventSubscription {
    public QuizSubscribesExecutionDeleted(Quiz quiz) {
        super(quiz.getAggregateId(), 0, ExecutionDeletedEvent.class);
    }
}
