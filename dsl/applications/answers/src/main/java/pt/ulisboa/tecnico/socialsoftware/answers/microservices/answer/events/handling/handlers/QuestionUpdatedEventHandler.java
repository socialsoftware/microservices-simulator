package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.AnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionUpdatedEvent;

public class QuestionUpdatedEventHandler extends AnswerEventHandler {
    public QuestionUpdatedEventHandler(AnswerRepository answerRepository, AnswerEventProcessing answerEventProcessing) {
        super(answerRepository, answerEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.answerEventProcessing.processQuestionUpdatedEvent(subscriberAggregateId, (QuestionUpdatedEvent) event);
    }
}
