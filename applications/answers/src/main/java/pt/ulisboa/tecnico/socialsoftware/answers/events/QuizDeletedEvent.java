package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuizDeletedEvent extends Event {

    public QuizDeletedEvent() {
        super();
    }

    public QuizDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }


}