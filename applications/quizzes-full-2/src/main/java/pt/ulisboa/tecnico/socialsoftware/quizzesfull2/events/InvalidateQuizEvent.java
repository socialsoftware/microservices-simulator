package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class InvalidateQuizEvent extends Event {
    private Integer quizAggregateId;

    protected InvalidateQuizEvent() {}

    public InvalidateQuizEvent(Integer quizAggregateId) {
        super(quizAggregateId);
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }
}
