package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class AnswerQuizUpdatedEvent extends Event {
    @Column(name = "answer_quiz_updated_event_quiz_aggregate_id")
    private Integer quizAggregateId;
    @Column(name = "answer_quiz_updated_event_quiz_version")
    private Integer quizVersion;

    public AnswerQuizUpdatedEvent() {
        super();
    }

    public AnswerQuizUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AnswerQuizUpdatedEvent(Integer aggregateId, Integer quizAggregateId, Integer quizVersion) {
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