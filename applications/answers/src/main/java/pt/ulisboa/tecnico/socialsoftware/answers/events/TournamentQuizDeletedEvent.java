package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentQuizDeletedEvent extends Event {
    private Integer quizAggregateId;

    public TournamentQuizDeletedEvent() {
        super();
    }

    public TournamentQuizDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentQuizDeletedEvent(Integer aggregateId, Integer quizAggregateId) {
        super(aggregateId);
        setQuizAggregateId(quizAggregateId);
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

}