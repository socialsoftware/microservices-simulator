package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.AnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answerexecution.events.publish.DeleteExecutionEvent;

public class DeleteExecutionEventHandler extends AnswerEventHandler {
    public DeleteExecutionEventHandler(AnswerRepository answerRepository, AnswerEventProcessing answerEventProcessing) {
        super(answerRepository, answerEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.answerEventProcessing.processDeleteExecutionEvent(subscriberAggregateId, (DeleteExecutionEvent) event);
    }
}
