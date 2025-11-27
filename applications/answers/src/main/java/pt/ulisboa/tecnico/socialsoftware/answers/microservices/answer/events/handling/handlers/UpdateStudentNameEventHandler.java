package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.AnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answeruser.events.publish.UpdateStudentNameEvent;

public class UpdateStudentNameEventHandler extends AnswerEventHandler {
    public UpdateStudentNameEventHandler(AnswerRepository answerRepository, AnswerEventProcessing answerEventProcessing) {
        super(answerRepository, answerEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.answerEventProcessing.processUpdateStudentNameEvent(subscriberAggregateId, (UpdateStudentNameEvent) event);
    }
}
