package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_participant")
public class TournamentParticipant {

    @Id
    @GeneratedValue
    private Integer id;

    private Integer participantAggregateId;
    private String participantName;
    private String participantUsername;
    private Long participantVersion;
    private LocalDateTime enrollTime;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private TournamentParticipantQuizAnswer quizAnswer;

    public TournamentParticipant() {}

    public TournamentParticipant(Integer participantAggregateId, String participantName,
                                  String participantUsername, Long participantVersion,
                                  LocalDateTime enrollTime) {
        this.participantAggregateId = participantAggregateId;
        this.participantName = participantName;
        this.participantUsername = participantUsername;
        this.participantVersion = participantVersion;
        this.enrollTime = enrollTime;
        this.quizAnswer = new TournamentParticipantQuizAnswer();
    }

    public TournamentParticipant(TournamentParticipant other) {
        this.participantAggregateId = other.getParticipantAggregateId();
        this.participantName = other.getParticipantName();
        this.participantUsername = other.getParticipantUsername();
        this.participantVersion = other.getParticipantVersion();
        this.enrollTime = other.getEnrollTime();
        this.quizAnswer = new TournamentParticipantQuizAnswer(other.getQuizAnswer());
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getParticipantAggregateId() { return participantAggregateId; }
    public void setParticipantAggregateId(Integer participantAggregateId) { this.participantAggregateId = participantAggregateId; }

    public String getParticipantName() { return participantName; }
    public void setParticipantName(String participantName) { this.participantName = participantName; }

    public String getParticipantUsername() { return participantUsername; }
    public void setParticipantUsername(String participantUsername) { this.participantUsername = participantUsername; }

    public Long getParticipantVersion() { return participantVersion; }
    public void setParticipantVersion(Long participantVersion) { this.participantVersion = participantVersion; }

    public LocalDateTime getEnrollTime() { return enrollTime; }
    public void setEnrollTime(LocalDateTime enrollTime) { this.enrollTime = enrollTime; }

    public TournamentParticipantQuizAnswer getQuizAnswer() { return quizAnswer; }
    public void setQuizAnswer(TournamentParticipantQuizAnswer quizAnswer) { this.quizAnswer = quizAnswer; }
}
