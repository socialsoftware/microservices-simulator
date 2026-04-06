package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
    @OneToOne
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
    }

    public TournamentParticipant(TournamentParticipant other) {
        setParticipantAggregateId(other.getParticipantAggregateId());
        setParticipantVersion(other.getParticipantVersion());
        setParticipantState(other.getParticipantState());
        setParticipantEnrollTime(other.getParticipantEnrollTime());
        setParticipantQuiz(new TournamentParticipantQuiz(other.getParticipantQuiz()));
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
        return dto;
    }
}