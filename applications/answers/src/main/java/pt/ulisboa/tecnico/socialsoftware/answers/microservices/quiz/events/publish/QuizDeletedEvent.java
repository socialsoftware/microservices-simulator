package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuizDeletedEvent extends Event {

    public QuizDeletedEvent() {
    }

    public QuizDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

}