package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

@Entity
public class TournamentCreator {
    @Id
    @GeneratedValue
    private Long id;
    private Integer creatorAggregateId;
    private String creatorName;
    private String creatorUsername;
    private Integer creatorVersion;
    private Aggregate.AggregateState creatorState;

    @OneToOne
    private Tournament tournament;

    public TournamentCreator() {

    }
    public TournamentCreator(Integer creatorAggregateId, String creatorName, String creatorUsername, Integer creatorVersion) {
        setCreatorAggregateId(creatorAggregateId);
        setCreatorName(creatorName);
        setCreatorUsername(creatorUsername);
        setCreatorVersion(creatorVersion);
    }

    public TournamentCreator(TournamentCreator other) {
        setCreatorAggregateId(other.getCreatorAggregateId());
        setCreatorName(other.getCreatorName());
        setCreatorUsername(other.getCreatorUsername());
        setCreatorVersion(other.getCreatorVersion());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getCreatorAggregateId() {
        return creatorAggregateId;
    }

    public void setCreatorAggregateId(Integer id) {
        this.creatorAggregateId = id;
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

    public Aggregate.AggregateState getCreatorState() {
        return creatorState;
    }

    public void setCreatorState(Aggregate.AggregateState creatorState) {
        this.creatorState = creatorState;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public UserDto buildDto() {
        UserDto userDto = new UserDto();
        userDto.setAggregateId(getCreatorAggregateId());
        userDto.setVersion(getCreatorVersion());
        userDto.setName(getCreatorName());
        userDto.setUsername(getCreatorUsername());

        return userDto;
    }
}
