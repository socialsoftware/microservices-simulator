package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuestionDeletedEvent extends Event {

    public QuestionDeletedEvent() {
        super();
    }

    public QuestionDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

}