package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;

public class TournamentCreatorDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String state;
    private String name;
    private String username;
    private Integer executionUserAggregateId;
    private Integer executionUserVersion;
    private String executionUserState;

    public TournamentCreatorDto() {
    }

    public TournamentCreatorDto(TournamentCreator tournamentCreator) {
        this.aggregateId = tournamentCreator.getCreatorAggregateId();
        this.version = tournamentCreator.getCreatorVersion();
        this.state = tournamentCreator.getCreatorState() != null ? tournamentCreator.getCreatorState().name() : null;
        this.name = tournamentCreator.getCreatorName();
        this.username = tournamentCreator.getCreatorUsername();
        this.executionUserAggregateId = tournamentCreator.getExecutionUserAggregateId();
        this.executionUserVersion = tournamentCreator.getExecutionUserVersion();
        this.executionUserState = tournamentCreator.getExecutionUserState() != null ? tournamentCreator.getExecutionUserState().name() : null;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getExecutionUserAggregateId() {
        return executionUserAggregateId;
    }

    public void setExecutionUserAggregateId(Integer executionUserAggregateId) {
        this.executionUserAggregateId = executionUserAggregateId;
    }

    public Integer getExecutionUserVersion() {
        return executionUserVersion;
    }

    public void setExecutionUserVersion(Integer executionUserVersion) {
        this.executionUserVersion = executionUserVersion;
    }

    public String getExecutionUserState() {
        return executionUserState;
    }

    public void setExecutionUserState(String executionUserState) {
        this.executionUserState = executionUserState;
    }
}