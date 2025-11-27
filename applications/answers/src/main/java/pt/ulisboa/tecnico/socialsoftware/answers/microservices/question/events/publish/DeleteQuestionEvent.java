package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DeleteQuestionEvent extends Event {

    public DeleteQuestionEvent() {
    }

    public DeleteQuestionEvent(Integer aggregateId) {
        super(aggregateId);
    }

}