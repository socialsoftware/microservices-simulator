package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentCreatorDto;

@Entity
public class TournamentCreator {
    @Id
    @GeneratedValue
    private Long id;
    private Integer creatorAggregateId;
    private Integer creatorVersion;
    private AggregateState creatorState;
    private String creatorName;
    private String creatorUsername;
    private Integer executionUserAggregateId;
    private Integer executionUserVersion;
    private AggregateState executionUserState;
    @OneToOne
    private Tournament tournament;

    public TournamentCreator() {

    }

    public TournamentCreator(ExecutionUserDto executionUserDto) {
        setCreatorAggregateId(executionUserDto.getAggregateId());
        setCreatorVersion(executionUserDto.getVersion());
        setCreatorState(executionUserDto.getState() != null ? AggregateState.valueOf(executionUserDto.getState()) : null);
    }

    public TournamentCreator(TournamentCreatorDto tournamentCreatorDto) {
        setCreatorAggregateId(tournamentCreatorDto.getAggregateId());
        setCreatorVersion(tournamentCreatorDto.getVersion());
        setCreatorState(tournamentCreatorDto.getState() != null ? AggregateState.valueOf(tournamentCreatorDto.getState()) : null);
        setCreatorName(tournamentCreatorDto.getName());
        setCreatorUsername(tournamentCreatorDto.getUsername());
        setExecutionUserAggregateId(tournamentCreatorDto.getExecutionUserAggregateId());
        setExecutionUserVersion(tournamentCreatorDto.getExecutionUserVersion());
        setExecutionUserState(tournamentCreatorDto.getExecutionUserState() != null ? AggregateState.valueOf(tournamentCreatorDto.getExecutionUserState()) : null);
    }

    public TournamentCreator(TournamentCreator other) {
        setCreatorAggregateId(other.getCreatorAggregateId());
        setCreatorVersion(other.getCreatorVersion());
        setCreatorState(other.getCreatorState());
        setCreatorName(other.getCreatorName());
        setCreatorUsername(other.getCreatorUsername());
        setExecutionUserAggregateId(other.getExecutionUserAggregateId());
        setExecutionUserVersion(other.getExecutionUserVersion());
        setExecutionUserState(other.getExecutionUserState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCreatorAggregateId() {
        return creatorAggregateId;
    }

    public void setCreatorAggregateId(Integer creatorAggregateId) {
        this.creatorAggregateId = creatorAggregateId;
    }

    public Integer getCreatorVersion() {
        return creatorVersion;
    }

    public void setCreatorVersion(Integer creatorVersion) {
        this.creatorVersion = creatorVersion;
    }

    public AggregateState getCreatorState() {
        return creatorState;
    }

    public void setCreatorState(AggregateState creatorState) {
        this.creatorState = creatorState;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
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

    public AggregateState getExecutionUserState() {
        return executionUserState;
    }

    public void setExecutionUserState(AggregateState executionUserState) {
        this.executionUserState = executionUserState;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }




    public TournamentCreatorDto buildDto() {
        TournamentCreatorDto dto = new TournamentCreatorDto();
        dto.setAggregateId(getCreatorAggregateId());
        dto.setVersion(getCreatorVersion());
        dto.setState(getCreatorState() != null ? getCreatorState().name() : null);
        dto.setName(getCreatorName());
        dto.setUsername(getCreatorUsername());
        return dto;
    }
}