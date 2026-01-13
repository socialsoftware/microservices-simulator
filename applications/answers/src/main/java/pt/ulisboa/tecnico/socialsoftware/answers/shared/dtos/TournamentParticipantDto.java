package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;

public class TournamentParticipantDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String username;
    private LocalDateTime participantEnrollTime;
    private TournamentParticipantQuizDto participantQuiz;

    public TournamentParticipantDto() {
    }

    public TournamentParticipantDto(TournamentParticipant tournamentParticipant) {
        this.aggregateId = tournamentParticipant.getParticipantAggregateId();
        this.version = tournamentParticipant.getParticipantVersion();
        this.state = tournamentParticipant.getParticipantState();
        this.name = tournamentParticipant.getParticipantName();
        this.username = tournamentParticipant.getParticipantUsername();
        this.participantEnrollTime = tournamentParticipant.getParticipantEnrollTime();
        this.participantQuiz = tournamentParticipant.getParticipantQuiz() != null ? new TournamentParticipantQuizDto(tournamentParticipant.getParticipantQuiz()) : null;
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

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
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

    public LocalDateTime getParticipantEnrollTime() {
        return participantEnrollTime;
    }

    public void setParticipantEnrollTime(LocalDateTime participantEnrollTime) {
        this.participantEnrollTime = participantEnrollTime;
    }

    public TournamentParticipantQuizDto getParticipantQuiz() {
        return participantQuiz;
    }

    public void setParticipantQuiz(TournamentParticipantQuizDto participantQuiz) {
        this.participantQuiz = participantQuiz;
    }
}