package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;

public class TournamentParticipantDto implements Serializable {
    private LocalDateTime participantEnrollTime;
    private TournamentParticipantQuizDto participantQuiz;
    private Integer aggregateId;
    private String name;
    private String username;
    private Integer version;
    private AggregateState state;

    public TournamentParticipantDto() {
    }

    public TournamentParticipantDto(TournamentParticipant tournamentParticipant) {
        this.participantEnrollTime = tournamentParticipant.getParticipantEnrollTime();
        this.participantQuiz = tournamentParticipant.getParticipantQuiz() != null ? new TournamentParticipantQuizDto(tournamentParticipant.getParticipantQuiz()) : null;
        this.aggregateId = tournamentParticipant.getParticipantAggregateId();
        this.name = tournamentParticipant.getParticipantName();
        this.username = tournamentParticipant.getParticipantUsername();
        this.version = tournamentParticipant.getParticipantVersion();
        this.state = tournamentParticipant.getParticipantState();
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

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
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
}