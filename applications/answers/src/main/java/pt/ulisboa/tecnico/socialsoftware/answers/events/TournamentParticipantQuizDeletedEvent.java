package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentParticipantQuizDeletedEvent extends Event {
    private Integer quizAggregateId;

    public TournamentParticipantQuizDeletedEvent() {
        super();
    }

    public TournamentParticipantQuizDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentParticipantQuizDeletedEvent(Integer aggregateId, Integer quizAggregateId) {
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