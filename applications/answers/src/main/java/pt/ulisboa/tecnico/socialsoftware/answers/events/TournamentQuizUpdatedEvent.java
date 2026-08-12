package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentQuizUpdatedEvent extends Event {
    private Integer quizAggregateId;
    private Integer quizVersion;

    public TournamentQuizUpdatedEvent() {
        super();
    }

    public TournamentQuizUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentQuizUpdatedEvent(Integer aggregateId, Integer quizAggregateId, Integer quizVersion) {
        super(aggregateId);
        setQuizAggregateId(quizAggregateId);
        setQuizVersion(quizVersion);
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Integer quizVersion) {
        this.quizVersion = quizVersion;
    }

}