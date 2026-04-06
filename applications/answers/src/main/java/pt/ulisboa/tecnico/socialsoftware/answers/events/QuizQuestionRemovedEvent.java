package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class QuizQuestionRemovedEvent extends Event {
    @Column(name = "quiz_question_removed_event_question_aggregate_id")
    private Integer questionAggregateId;

    public QuizQuestionRemovedEvent() {
        super();
    }

    public QuizQuestionRemovedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuizQuestionRemovedEvent(Integer aggregateId, Integer questionAggregateId) {
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