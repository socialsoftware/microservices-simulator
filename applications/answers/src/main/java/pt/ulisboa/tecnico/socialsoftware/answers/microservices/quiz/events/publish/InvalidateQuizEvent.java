package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class InvalidateQuizEvent extends Event {

    public InvalidateQuizEvent() {
    }

    public InvalidateQuizEvent(Integer aggregateId) {
        super(aggregateId);
    }

}