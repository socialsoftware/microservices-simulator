package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

@Entity
public class TournamentExecution {
    @Id
    @GeneratedValue
    private Long id;
    private Integer executionAggregateId;
    private Integer executionAggregateCourseId;
    private String executionAcronym;
    private AggregateState executionState;
    private Integer executionVersion;
    @OneToOne
    private Tournament tournament;

    public TournamentExecution() {
    }

    public TournamentExecution(ExecutionDto executionDto) {
        setExecutionAggregateId(executionDto.getAggregateId());
        setExecutionAggregateCourseId(executionDto.getCourseAggregateId());
        setExecutionAcronym(executionDto.getAcronym());
        setExecutionState(executionDto.getState());
        setExecutionVersion(executionDto.getVersion());
    }

    public TournamentExecution(TournamentExecution other) {
        setExecutionAggregateId(other.getExecutionAggregateId());
        setExecutionAggregateCourseId(other.getExecutionAggregateCourseId());
        setExecutionAcronym(other.getExecutionAcronym());
        setExecutionState(other.getExecutionState());
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

    public Integer getExecutionAggregateCourseId() {
        return executionAggregateCourseId;
    }

    public void setExecutionAggregateCourseId(Integer executionAggregateCourseId) {
        this.executionAggregateCourseId = executionAggregateCourseId;
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


}