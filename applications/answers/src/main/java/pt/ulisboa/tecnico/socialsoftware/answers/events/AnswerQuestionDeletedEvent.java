package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnswerQuestionDeletedEvent extends Event {
    private Integer questionAggregateId;

    public AnswerQuestionDeletedEvent() {
        super();
    }

    public AnswerQuestionDeletedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AnswerQuestionDeletedEvent(Integer aggregateId, Integer questionAggregateId) {
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