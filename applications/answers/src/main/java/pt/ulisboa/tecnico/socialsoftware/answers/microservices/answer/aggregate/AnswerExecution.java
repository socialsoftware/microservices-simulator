package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

@Entity
public class AnswerExecution {
    @Id
    @GeneratedValue
    private Long id;
    private Integer executionAggregateId;
    private Integer executionVersion;
    @OneToOne
    private Answer answer;

    public AnswerExecution() {
    }

    public AnswerExecution(ExecutionDto executionDto) {
        setExecutionAggregateId(executionDto.getAggregateId());
        setExecutionVersion(executionDto.getVersion());
    }

    public AnswerExecution(AnswerExecution other) {
        setExecutionAggregateId(other.getExecutionAggregateId());
        setExecutionVersion(other.getExecutionVersion());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Integer executionVersion) {
        this.executionVersion = executionVersion;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

}