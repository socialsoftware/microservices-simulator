package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

@Entity
public class AnswerExecution {
    @Id
    @GeneratedValue
    private Long id;
    private Integer executionAggregateId;
    private Integer executionVersion;
    private AggregateState executionState;
    @OneToOne
    private Answer answer;

    public AnswerExecution() {

    }

    public AnswerExecution(ExecutionDto executionDto) {
        setExecutionAggregateId(executionDto.getAggregateId());
        setExecutionVersion(executionDto.getVersion());
        setExecutionState(executionDto.getState());
    }

    public AnswerExecution(AnswerExecutionDto answerExecutionDto) {
        setExecutionAggregateId(answerExecutionDto.getAggregateId());
        setExecutionVersion(answerExecutionDto.getVersion());
        setExecutionState(answerExecutionDto.getState());
    }

    public AnswerExecution(AnswerExecution other) {
        setExecutionVersion(other.getExecutionVersion());
        setExecutionState(other.getExecutionState());
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

    public AggregateState getExecutionState() {
        return executionState;
    }

    public void setExecutionState(AggregateState executionState) {
        this.executionState = executionState;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }



    public AnswerExecutionDto buildDto() {
        AnswerExecutionDto dto = new AnswerExecutionDto();
        dto.setAggregateId(getExecutionAggregateId());
        dto.setVersion(getExecutionVersion());
        dto.setState(getExecutionState());
        return dto;
    }
}