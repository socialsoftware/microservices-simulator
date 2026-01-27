package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;

public class AnswerExecutionDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public AnswerExecutionDto() {
    }

    public AnswerExecutionDto(AnswerExecution answerExecution) {
        this.aggregateId = answerExecution.getExecutionAggregateId();
        this.version = answerExecution.getExecutionVersion();
        this.state = answerExecution.getExecutionState();
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