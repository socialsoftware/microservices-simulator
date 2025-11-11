package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;

@Entity
public class TournamentCreator {
    @Id
    @GeneratedValue
    private Long id;
    private Integer creatorAggregateId;
    private String creatorName;
    private String creatorUsername;
    private Integer creatorVersion;
    private AggregateState creatorState;
    @OneToOne
    private Tournament tournament;

    public TournamentCreator() {

    }

    public TournamentCreator(UserDto userDto) {
        setCreatorAggregateId(userDto.getAggregateId());
        setCreatorName(userDto.getName());
        setCreatorUsername(userDto.getUsername());
        setCreatorVersion(userDto.getVersion());
        setCreatorState(userDto.getState());
    }

    public TournamentCreator(TournamentCreator other) {
        setCreatorAggregateId(other.getCreatorAggregateId());
        setCreatorName(other.getCreatorName());
        setCreatorUsername(other.getCreatorUsername());
        setCreatorVersion(other.getCreatorVersion());
        setCreatorState(other.getCreatorState());
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

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


    public UserDto buildDto() {
        UserDto dto = new UserDto();
        dto.setAggregateId(getCreatorAggregateId());
        dto.setName(getCreatorName());
        dto.setUsername(getCreatorUsername());
        dto.setVersion(getCreatorVersion());
        dto.setState(getCreatorState());
        return dto;
    }
}