package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;

@Entity
public class TournamentParticipant {
    @Id
    @GeneratedValue
    private Long id;
    private Integer participantAggregateId;
    private Integer participantVersion;
    private AggregateState participantState;
    private String participantName;
    private String participantUsername;
    private LocalDateTime participantEnrollTime;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentparticipant")
    private TournamentParticipantQuizAnswer tournamentParticipantQuizAnswer;
    @OneToOne
    private Tournament tournament;

    public TournamentParticipant() {

    }

    public TournamentParticipant(UserDto userDto) {
        setParticipantAggregateId(userDto.getAggregateId());
        setParticipantVersion(userDto.getVersion());
        setParticipantState(userDto.getState());
        setParticipantName(userDto.getName());
        setParticipantUsername(userDto.getUsername());
    }

    public TournamentParticipant(TournamentParticipant other) {
        setParticipantAggregateId(other.getParticipantAggregateId());
        setParticipantVersion(other.getParticipantVersion());
        setParticipantState(other.getParticipantState());
        setParticipantName(other.getParticipantName());
        setParticipantUsername(other.getParticipantUsername());
        setParticipantEnrollTime(other.getParticipantEnrollTime());
        setTournamentParticipantQuizAnswer(new TournamentParticipantQuizAnswer(other.getTournamentParticipantQuizAnswer()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getParticipantAggregateId() {
        return participantAggregateId;
    }

    public void setParticipantAggregateId(Integer participantAggregateId) {
        this.participantAggregateId = participantAggregateId;
    }

    public Integer getParticipantVersion() {
        return participantVersion;
    }

    public void setParticipantVersion(Integer participantVersion) {
        this.participantVersion = participantVersion;
    }

    public AggregateState getParticipantState() {
        return participantState;
    }

    public void setParticipantState(AggregateState participantState) {
        this.participantState = participantState;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantUsername() {
        return participantUsername;
    }

    public void setParticipantUsername(String participantUsername) {
        this.participantUsername = participantUsername;
    }

    public LocalDateTime getParticipantEnrollTime() {
        return participantEnrollTime;
    }

    public void setParticipantEnrollTime(LocalDateTime participantEnrollTime) {
        this.participantEnrollTime = participantEnrollTime;
    }

    public TournamentParticipantQuizAnswer getTournamentParticipantQuizAnswer() {
        return tournamentParticipantQuizAnswer;
    }

    public void setTournamentParticipantQuizAnswer(TournamentParticipantQuizAnswer tournamentParticipantQuizAnswer) {
        this.tournamentParticipantQuizAnswer = tournamentParticipantQuizAnswer;
        if (this.tournamentParticipantQuizAnswer != null) {
            this.tournamentParticipantQuizAnswer.setTournamentParticipant(this);
        }
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

}