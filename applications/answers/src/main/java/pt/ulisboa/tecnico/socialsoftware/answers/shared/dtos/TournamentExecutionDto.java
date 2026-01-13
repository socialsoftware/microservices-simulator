package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;

public class TournamentExecutionDto implements Serializable {
    private Integer aggregateId;
    private Integer courseAggregateId;
    private String acronym;
    private AggregateState state;
    private Integer version;

    public TournamentExecutionDto() {
    }

    public TournamentExecutionDto(TournamentExecution tournamentExecution) {
        this.aggregateId = tournamentExecution.getExecutionAggregateId();
        this.courseAggregateId = tournamentExecution.getExecutionCourseAggregateId();
        this.acronym = tournamentExecution.getExecutionAcronym();
        this.state = tournamentExecution.getExecutionState();
        this.version = tournamentExecution.getExecutionVersion();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}