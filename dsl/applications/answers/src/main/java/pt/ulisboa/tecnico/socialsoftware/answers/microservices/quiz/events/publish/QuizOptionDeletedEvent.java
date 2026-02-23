package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuizOptionDeletedEvent extends Event {
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