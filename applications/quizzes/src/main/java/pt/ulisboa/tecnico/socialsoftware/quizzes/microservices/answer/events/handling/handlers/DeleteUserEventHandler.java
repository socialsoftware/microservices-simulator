package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteUserEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing;

public class DeleteUserEventHandler extends QuizAnswerEventHandler {
    public DeleteUserEventHandler(QuizAnswerRepository quizAnswerRepository, QuizAnswerEventProcessing quizAnswerEventProcessing) {
        super(quizAnswerRepository, quizAnswerEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.quizAnswerEventProcessing.processDeleteUserEvent(subscriberAggregateId, (DeleteUserEvent) event);
    }
}
