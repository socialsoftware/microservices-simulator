package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

@Entity
public class TournamentExecution {
    @Id
    @GeneratedValue
    private Long id;
    private Integer executionAggregateId;
    private Integer executionCourseAggregateId;
    private String executionAcronym;
    private AggregateState executionState;
    private Integer executionVersion;
    @OneToOne
    private Tournament tournament;

    public TournamentExecution() {

    }

    public TournamentExecution(ExecutionDto executionDto) {
        setExecutionAggregateId(executionDto.getAggregateId());
        setExecutionVersion(executionDto.getVersion());
        setExecutionState(executionDto.getState());
        setExecutionAcronym(executionDto.getAcronym());
    }

    public TournamentExecution(TournamentExecution other) {
        setExecutionCourseAggregateId(other.getExecutionCourseAggregateId());
        setExecutionAcronym(other.getExecutionAcronym());
        setExecutionState(other.getExecutionState());
        setExecutionVersion(other.getExecutionVersion());
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionCourseAggregateId() {
        return executionCourseAggregateId;
    }

    public void setExecutionCourseAggregateId(Integer executionCourseAggregateId) {
        this.executionCourseAggregateId = executionCourseAggregateId;
    }

    public String getExecutionAcronym() {
        return executionAcronym;
    }

    public void setExecutionAcronym(String executionAcronym) {
        this.executionAcronym = executionAcronym;
    }

    public AggregateState getExecutionState() {
        return executionState;
    }

    public void setExecutionState(AggregateState executionState) {
        this.executionState = executionState;
    }

    public Integer getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Integer executionVersion) {
        this.executionVersion = executionVersion;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


    public ExecutionDto buildDto() {
        ExecutionDto dto = new ExecutionDto();
        dto.setAggregateId(getExecutionAggregateId());
        dto.setVersion(getExecutionVersion());
        dto.setState(getExecutionState());
        dto.setAcronym(getExecutionAcronym());
        return dto;
    }
}