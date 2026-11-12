package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_participants")
public class TournamentParticipant {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer userAggregateId;
    private String userName;
    private String userUsername;
    private Long userVersion;
    private LocalDateTime enrollTime;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private TournamentParticipantQuizAnswer quizAnswer;

    public TournamentParticipant() {
    }

    public TournamentParticipant(Integer userAggregateId, String userName, String userUsername, Long userVersion,
                                 LocalDateTime enrollTime) {
        this.userAggregateId = userAggregateId;
        this.userName = userName;
        this.userUsername = userUsername;
        this.userVersion = userVersion;
        this.enrollTime = enrollTime;
        this.quizAnswer = new TournamentParticipantQuizAnswer();
    }

    public TournamentParticipant(TournamentParticipant other) {
        this.userAggregateId = other.getUserAggregateId();
        this.userName = other.getUserName();
        this.userUsername = other.getUserUsername();
        this.userVersion = other.getUserVersion();
        this.enrollTime = other.getEnrollTime();
        this.quizAnswer = new TournamentParticipantQuizAnswer(other.getQuizAnswer());
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

    public LocalDateTime getEnrollTime() {
        return enrollTime;
    }

    public void setEnrollTime(LocalDateTime enrollTime) {
        this.enrollTime = enrollTime;
    }

    public TournamentParticipantQuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(TournamentParticipantQuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}
