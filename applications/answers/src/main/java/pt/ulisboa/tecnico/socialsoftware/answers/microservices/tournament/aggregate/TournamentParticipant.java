package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantQuizDto;

@Entity
public class TournamentParticipant {
    @Id
    @GeneratedValue
    private Long id;
    private Integer participantAggregateId;
    private Integer participantVersion;
    private AggregateState participantState;
    private LocalDateTime participantEnrollTime;
    @OneToOne(cascade = CascadeType.ALL)
    private TournamentParticipantQuiz participantQuiz;
    private String participantName;
    private String participantUsername;
    private Integer executionUserAggregateId;
    private Integer executionUserVersion;
    private AggregateState executionUserState;
    @ManyToOne
    private Tournament tournament;

    public TournamentParticipant() {

    }

    public TournamentParticipant(ExecutionUserDto executionUserDto) {
        setParticipantAggregateId(executionUserDto.getAggregateId());
        setParticipantVersion(executionUserDto.getVersion());
        setParticipantState(executionUserDto.getState() != null ? AggregateState.valueOf(executionUserDto.getState()) : null);
    }

    public TournamentParticipant(TournamentParticipantDto tournamentParticipantDto) {
        setParticipantAggregateId(tournamentParticipantDto.getAggregateId());
        setParticipantVersion(tournamentParticipantDto.getVersion());
        setParticipantState(tournamentParticipantDto.getState() != null ? AggregateState.valueOf(tournamentParticipantDto.getState()) : null);
        setParticipantEnrollTime(tournamentParticipantDto.getParticipantEnrollTime());
        setParticipantQuiz(tournamentParticipantDto.getParticipantQuiz() != null ? new TournamentParticipantQuiz(tournamentParticipantDto.getParticipantQuiz()) : null);
        setParticipantName(tournamentParticipantDto.getName());
        setParticipantUsername(tournamentParticipantDto.getUsername());
        setExecutionUserAggregateId(tournamentParticipantDto.getExecutionUserAggregateId());
        setExecutionUserVersion(tournamentParticipantDto.getExecutionUserVersion());
        setExecutionUserState(tournamentParticipantDto.getExecutionUserState() != null ? AggregateState.valueOf(tournamentParticipantDto.getExecutionUserState()) : null);
    }

    public TournamentParticipant(TournamentParticipant other) {
        setParticipantAggregateId(other.getParticipantAggregateId());
        setParticipantVersion(other.getParticipantVersion());
        setParticipantState(other.getParticipantState());
        setParticipantEnrollTime(other.getParticipantEnrollTime());
        setParticipantQuiz(other.getParticipantQuiz() != null ? new TournamentParticipantQuiz(other.getParticipantQuiz()) : null);
        setParticipantName(other.getParticipantName());
        setParticipantUsername(other.getParticipantUsername());
        setExecutionUserAggregateId(other.getExecutionUserAggregateId());
        setExecutionUserVersion(other.getExecutionUserVersion());
        setExecutionUserState(other.getExecutionUserState());
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

    public LocalDateTime getParticipantEnrollTime() {
        return participantEnrollTime;
    }

    public void setParticipantEnrollTime(LocalDateTime participantEnrollTime) {
        this.participantEnrollTime = participantEnrollTime;
    }

    public TournamentParticipantQuiz getParticipantQuiz() {
        return participantQuiz;
    }

    public void setParticipantQuiz(TournamentParticipantQuiz participantQuiz) {
        this.participantQuiz = participantQuiz;
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

    public AggregateState getExecutionUserState() {
        return executionUserState;
    }

    public void setExecutionUserState(AggregateState executionUserState) {
        this.executionUserState = executionUserState;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }




    public TournamentParticipantDto buildDto() {
        TournamentParticipantDto dto = new TournamentParticipantDto();
        dto.setAggregateId(getParticipantAggregateId());
        dto.setVersion(getParticipantVersion());
        dto.setState(getParticipantState() != null ? getParticipantState().name() : null);
        dto.setParticipantEnrollTime(getParticipantEnrollTime());
        dto.setParticipantQuiz(getParticipantQuiz() != null ? new TournamentParticipantQuizDto(getParticipantQuiz()) : null);
        dto.setName(getParticipantName());
        dto.setUsername(getParticipantUsername());
        return dto;
    }
}