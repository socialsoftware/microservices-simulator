package pt.ulisboa.tecnico.socialsoftware.quizzes.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DeleteQuestionEvent extends Event {
    public DeleteQuestionEvent() {
        super();
    }

    public DeleteQuestionEvent(Integer questionAggregateId) {
        super(questionAggregateId);
    }
}
