package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tournament_creators")
public class TournamentCreator {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer userAggregateId;
    private String userName;
    private String userUsername;
    private Long userVersion;
    @OneToOne
    private Tournament tournament;

    public TournamentCreator() {
    }

    public TournamentCreator(Integer userAggregateId, String userName, String userUsername, Long userVersion) {
        this.userAggregateId = userAggregateId;
        this.userName = userName;
        this.userUsername = userUsername;
        this.userVersion = userVersion;
    }

    public TournamentCreator(TournamentCreator other) {
        this.userAggregateId = other.getUserAggregateId();
        this.userName = other.getUserName();
        this.userUsername = other.getUserUsername();
        this.userVersion = other.getUserVersion();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public Long getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Long userVersion) {
        this.userVersion = userVersion;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }
}
