package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing;

public class InvalidateQuizEventHandler extends QuizAnswerEventHandler {
    public InvalidateQuizEventHandler(QuizAnswerRepository quizAnswerRepository,
            QuizAnswerEventProcessing quizAnswerEventProcessing) {
        super(quizAnswerRepository, quizAnswerEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.quizAnswerEventProcessing.processInvalidateQuizEvent(subscriberAggregateId,
                (InvalidateQuizEvent) event);
    }
}
