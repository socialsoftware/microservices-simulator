package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class AnswerQuestionUpdatedEvent extends Event {
    private Integer questionAggregateId;
    private Integer questionVersion;
    private Integer sequence;
    private Integer key;
    private Integer timeTaken;
    private Boolean correct;

    public AnswerQuestionUpdatedEvent() {
        super();
    }

    public AnswerQuestionUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public AnswerQuestionUpdatedEvent(Integer aggregateId, Integer questionAggregateId, Integer questionVersion, Integer sequence, Integer key, Integer timeTaken, Boolean correct) {
        super(aggregateId);
        setQuestionAggregateId(questionAggregateId);
        setQuestionVersion(questionVersion);
        setSequence(sequence);
        setKey(key);
        setTimeTaken(timeTaken);
        setCorrect(correct);
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getQuestionVersion() {
        return questionVersion;
    }

    public void setQuestionVersion(Integer questionVersion) {
        this.questionVersion = questionVersion;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public Integer getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Integer timeTaken) {
        this.timeTaken = timeTaken;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

}