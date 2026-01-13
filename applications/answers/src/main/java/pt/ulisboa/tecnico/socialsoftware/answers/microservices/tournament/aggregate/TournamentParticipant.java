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
    private LocalDateTime participantEnrollTime;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentparticipant")
    private TournamentParticipantQuiz participantQuiz;
    private Integer participantAggregateId;
    private String participantName;
    private String participantUsername;
    private Integer participantVersion;
    private AggregateState participantState;
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
        setParticipantQuiz(new TournamentParticipantQuiz(other.getParticipantQuiz()));
        setParticipantAggregateId(other.getParticipantAggregateId());
        setParticipantName(other.getParticipantName());
        setParticipantUsername(other.getParticipantUsername());
        setParticipantVersion(other.getParticipantVersion());
        setParticipantState(other.getParticipantState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


    public TournamentParticipantDto buildDto() {
        TournamentParticipantDto dto = new TournamentParticipantDto();
        dto.setParticipantEnrollTime(getParticipantEnrollTime());
        dto.setParticipantQuiz(getParticipantQuiz() != null ? new TournamentParticipantQuizDto(getParticipantQuiz()) : null);
        dto.setAggregateId(getParticipantAggregateId());
        dto.setName(getParticipantName());
        dto.setUsername(getParticipantUsername());
        dto.setVersion(getParticipantVersion());
        dto.setState(getParticipantState());
        return dto;
    }
}