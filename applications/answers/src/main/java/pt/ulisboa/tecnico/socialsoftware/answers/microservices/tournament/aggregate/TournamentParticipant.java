package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import jakarta.persistence.CascadeType;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;

@Entity
public class TournamentParticipant {
    @Id
    @GeneratedValue
    private Long id;
    private Integer participantAggregateId;
    private String participantName;
    private String participantUsername;
    private LocalDateTime enrollTime;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentparticipant")
    private TournamentParticipantQuizAnswer participantAnswer;
    private Integer participantVersion;
    private AggregateState state;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentparticipant")
    private Tournament tournament;
    @OneToOne
    private Tournament tournament; 

    public TournamentParticipant() {
    }

    public TournamentParticipant(TournamentDto tournamentDto) {
        setParticipantAggregateId(tournamentDto.getParticipantAggregateId());
        setParticipantName(tournamentDto.getParticipantName());
        setParticipantUsername(tournamentDto.getParticipantUsername());
        setEnrollTime(tournamentDto.getEnrollTime());
        setParticipantAnswer(participantAnswer);
        setParticipantVersion(tournamentDto.getParticipantVersion());
        setState(tournamentDto.getState());
        setTournament(tournament);
    }

    public TournamentParticipant(TournamentParticipant other) {
        setParticipantAggregateId(other.getParticipantAggregateId());
        setParticipantName(other.getParticipantName());
        setParticipantUsername(other.getParticipantUsername());
        setEnrollTime(other.getEnrollTime());
        setParticipantAnswer(new TournamentParticipantQuizAnswer(other.getParticipantAnswer()));
        setParticipantVersion(other.getParticipantVersion());
        setState(other.getState());
        setTournament(new Tournament(other.getTournament()));
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

    public LocalDateTime getEnrollTime() {
        return enrollTime;
    }

    public void setEnrollTime(LocalDateTime enrollTime) {
        this.enrollTime = enrollTime;
    }

    public TournamentParticipantQuizAnswer getParticipantAnswer() {
        return participantAnswer;
    }

    public void setParticipantAnswer(TournamentParticipantQuizAnswer participantAnswer) {
        this.participantAnswer = participantAnswer;
        if (this.participantAnswer != null) {
            this.participantAnswer.setTournamentParticipant(this);
        }
    }

    public Integer getParticipantVersion() {
        return participantVersion;
    }

    public void setParticipantVersion(Integer participantVersion) {
        this.participantVersion = participantVersion;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
        if (this.tournament != null) {
            this.tournament.setTournamentParticipant(this);
        }
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


}