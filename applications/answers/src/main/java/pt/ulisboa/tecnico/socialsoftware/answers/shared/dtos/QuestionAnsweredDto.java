package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.QuestionAnswered;

public class QuestionAnsweredDto implements Serializable {
    private Integer sequence;
    private Integer key;
    private Integer timeTaken;
    private Boolean correct;
    private Integer questionAggregateId;
    private Integer questionVersion;
    private AggregateState state;

    public QuestionAnsweredDto() {
    }

    public QuestionAnsweredDto(QuestionAnswered questionAnswered) {
        this.sequence = questionAnswered.getSequence();
        this.key = questionAnswered.getKey();
        this.timeTaken = questionAnswered.getTimeTaken();
        this.correct = questionAnswered.getCorrect();
        this.questionAggregateId = questionAnswered.getQuestionAggregateId();
        this.questionVersion = questionAnswered.getQuestionVersion();
        this.state = questionAnswered.getState();
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}