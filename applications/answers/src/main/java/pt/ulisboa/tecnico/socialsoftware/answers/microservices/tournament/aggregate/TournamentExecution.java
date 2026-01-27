package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentExecutionDto;

@Entity
public class TournamentExecution {
    @Id
    @GeneratedValue
    private Long id;
    private AggregateState executionState;
    private Integer executionVersion;
    private Integer executionCourseAggregateId;
    private String executionAcronym;
    private Integer executionAggregateId;
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

    public TournamentExecution(TournamentExecutionDto tournamentExecutionDto) {
        setExecutionState(tournamentExecutionDto.getState());
        setExecutionVersion(tournamentExecutionDto.getVersion());
        setExecutionCourseAggregateId(tournamentExecutionDto.getCourseAggregateId());
        setExecutionAcronym(tournamentExecutionDto.getAcronym());
        setExecutionAggregateId(tournamentExecutionDto.getAggregateId());
    }

    public TournamentExecution(TournamentExecution other) {
        setExecutionVersion(other.getExecutionVersion());
        setExecutionCourseAggregateId(other.getExecutionCourseAggregateId());
        setExecutionAcronym(other.getExecutionAcronym());
        setExecutionAggregateId(other.getExecutionAggregateId());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }



    public TournamentExecutionDto buildDto() {
        TournamentExecutionDto dto = new TournamentExecutionDto();
        dto.setState(getExecutionState());
        dto.setVersion(getExecutionVersion());
        dto.setCourseAggregateId(getExecutionCourseAggregateId());
        dto.setAcronym(getExecutionAcronym());
        dto.setAggregateId(getExecutionAggregateId());
        return dto;
    }
}