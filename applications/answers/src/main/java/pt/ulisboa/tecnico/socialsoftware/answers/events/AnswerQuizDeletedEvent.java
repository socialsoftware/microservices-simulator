package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class AnswerQuizDeletedEvent extends Event {
    @Column(name = "answer_quiz_deleted_event_quiz_aggregate_id")
    private Integer quizAggregateId;

    public AnswerQuizDeletedEvent() {
        super();
    }

    public AnswerQuizDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AnswerQuizDeletedEvent(Integer aggregateId, Integer quizAggregateId) {
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