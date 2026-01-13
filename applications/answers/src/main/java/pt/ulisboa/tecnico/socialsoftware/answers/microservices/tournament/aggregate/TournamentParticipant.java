package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantQuizDto;
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
    private TournamentParticipantQuiz participantQuiz;
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
        setParticipantVersion(other.getParticipantVersion());
        setParticipantState(other.getParticipantState());
        setParticipantName(other.getParticipantName());
        setParticipantUsername(other.getParticipantUsername());
        setParticipantEnrollTime(other.getParticipantEnrollTime());
        setParticipantQuiz(new TournamentParticipantQuiz(other.getParticipantQuiz()));
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

    public TournamentParticipantQuiz getParticipantQuiz() {
        return participantQuiz;
    }

    public void setParticipantQuiz(TournamentParticipantQuiz participantQuiz) {
        this.participantQuiz = participantQuiz;
        if (this.participantQuiz != null) {
            this.participantQuiz.setTournamentParticipant(this);
        }
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
        dto.setState(getParticipantState());
        dto.setName(getParticipantName());
        dto.setUsername(getParticipantUsername());
        dto.setParticipantEnrollTime(getParticipantEnrollTime());
        dto.setParticipantQuiz(getParticipantQuiz() != null ? new TournamentParticipantQuizDto(getParticipantQuiz()) : null);
        return dto;
    }
}