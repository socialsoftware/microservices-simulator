package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentQuizDeletedEvent extends Event {
    @Column(name = "tournament_quiz_deleted_event_quiz_aggregate_id")
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