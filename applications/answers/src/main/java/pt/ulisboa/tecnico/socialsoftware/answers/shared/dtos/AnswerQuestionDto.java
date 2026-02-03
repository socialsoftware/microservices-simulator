package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuestion;

public class AnswerQuestionDto implements Serializable {
    private Integer sequence;
    private Integer key;
    private Integer timeTaken;
    private Boolean correct;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public AnswerQuestionDto() {
    }

    public AnswerQuestionDto(AnswerQuestion answerQuestion) {
        this.sequence = answerQuestion.getSequence();
        this.key = answerQuestion.getKey();
        this.timeTaken = answerQuestion.getTimeTaken();
        this.correct = answerQuestion.getCorrect();
        this.aggregateId = answerQuestion.getQuestionAggregateId();
        this.version = answerQuestion.getQuestionVersion();
        this.state = answerQuestion.getQuestionState();
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

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}