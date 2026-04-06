package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class QuizOptionDeletedEvent extends Event {
    @Column(name = "quiz_option_deleted_event_question_aggregate_id")
    private Integer questionAggregateId;

    public QuizOptionDeletedEvent() {
        super();
    }

    public QuizOptionDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuizOptionDeletedEvent(Integer aggregateId, Integer questionAggregateId) {
        super(aggregateId);
        setQuestionAggregateId(questionAggregateId);
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

}