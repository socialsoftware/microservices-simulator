package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.eventProcessing.AnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuizDeletedEvent;

public class QuizDeletedEventHandler extends AnswerEventHandler {
    public QuizDeletedEventHandler(AnswerRepository answerRepository, AnswerEventProcessing answerEventProcessing) {
        super(answerRepository, answerEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.answerEventProcessing.processQuizDeletedEvent(subscriberAggregateId, (QuizDeletedEvent) event);
    }
}
