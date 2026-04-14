package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;

public class TournamentParticipantDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String state;
    private LocalDateTime participantEnrollTime;
    private TournamentParticipantQuizDto participantQuiz;
    private String name;
    private String username;
    private Integer executionUserAggregateId;
    private Integer executionUserVersion;
    private String executionUserState;

    public TournamentParticipantDto() {
    }

    public TournamentParticipantDto(TournamentParticipant tournamentParticipant) {
        this.aggregateId = tournamentParticipant.getParticipantAggregateId();
        this.version = tournamentParticipant.getParticipantVersion();
        this.state = tournamentParticipant.getParticipantState() != null ? tournamentParticipant.getParticipantState().name() : null;
        this.participantEnrollTime = tournamentParticipant.getParticipantEnrollTime();
        this.participantQuiz = tournamentParticipant.getParticipantQuiz() != null ? new TournamentParticipantQuizDto(tournamentParticipant.getParticipantQuiz()) : null;
        this.name = tournamentParticipant.getParticipantName();
        this.username = tournamentParticipant.getParticipantUsername();
        this.executionUserAggregateId = tournamentParticipant.getExecutionUserAggregateId();
        this.executionUserVersion = tournamentParticipant.getExecutionUserVersion();
        this.executionUserState = tournamentParticipant.getExecutionUserState() != null ? tournamentParticipant.getExecutionUserState().name() : null;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public Integer getExecutionUserAggregateId() {
        return executionUserAggregateId;
    }

    public void setExecutionUserAggregateId(Integer executionUserAggregateId) {
        this.executionUserAggregateId = executionUserAggregateId;
    }

    public Integer getExecutionUserVersion() {
        return executionUserVersion;
    }

    public void setExecutionUserVersion(Integer executionUserVersion) {
        this.executionUserVersion = executionUserVersion;
    }

    public String getExecutionUserState() {
        return executionUserState;
    }

    public void setExecutionUserState(String executionUserState) {
        this.executionUserState = executionUserState;
    }
}