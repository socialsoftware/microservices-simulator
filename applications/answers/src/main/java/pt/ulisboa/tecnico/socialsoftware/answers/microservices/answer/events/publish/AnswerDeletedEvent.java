package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnswerDeletedEvent extends Event {

    public AnswerDeletedEvent() {
        super();
    }

    public AnswerDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

}