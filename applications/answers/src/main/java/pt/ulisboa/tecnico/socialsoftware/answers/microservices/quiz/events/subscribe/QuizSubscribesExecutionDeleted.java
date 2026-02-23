package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.handling.handlers.ExecutionDeletedEventHandler;

public class QuizSubscribesExecutionDeleted extends EventSubscription {
    public QuizSubscribesExecutionDeleted(Quiz quiz) {
        super(quiz,
                ExecutionDeletedEvent.class,
                ExecutionDeletedEventHandler.class);
    }
}
