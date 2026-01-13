package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;

public class AnswerExecutionDto implements Serializable {
    private Integer aggregateId;
    private Integer version;

    public AnswerExecutionDto() {
    }

    public AnswerExecutionDto(AnswerExecution answerExecution) {
        this.aggregateId = answerExecution.getExecutionAggregateId();
        this.version = answerExecution.getExecutionVersion();
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
}