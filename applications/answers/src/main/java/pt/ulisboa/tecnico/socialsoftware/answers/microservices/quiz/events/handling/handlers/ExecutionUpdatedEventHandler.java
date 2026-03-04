package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.eventProcessing.QuizEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUpdatedEvent;

public class ExecutionUpdatedEventHandler extends QuizEventHandler {
    public ExecutionUpdatedEventHandler(QuizRepository quizRepository, QuizEventProcessing quizEventProcessing) {
        super(quizRepository, quizEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.quizEventProcessing.processExecutionUpdatedEvent(subscriberAggregateId, (ExecutionUpdatedEvent) event);
    }
}
