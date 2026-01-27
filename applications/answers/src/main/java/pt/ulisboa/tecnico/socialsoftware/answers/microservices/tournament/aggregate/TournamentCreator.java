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
    @OneToOne
    private Tournament tournament;

    public TournamentCreator() {

    }

    public TournamentCreator(ExecutionUserDto executionUserDto) {
        setCreatorName(executionUserDto.getName());
        setCreatorUsername(executionUserDto.getUsername());
        setCreatorAggregateId(executionUserDto.getAggregateId());
        setCreatorVersion(executionUserDto.getVersion());
        setCreatorState(executionUserDto.getState());
    }

    public TournamentCreator(TournamentCreator other) {
        setCreatorVersion(other.getCreatorVersion());
        setCreatorState(other.getCreatorState());
        setCreatorName(other.getCreatorName());
        setCreatorUsername(other.getCreatorUsername());
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
        dto.setState(getCreatorState());
        dto.setName(getCreatorName());
        dto.setUsername(getCreatorUsername());
        return dto;
    }
}