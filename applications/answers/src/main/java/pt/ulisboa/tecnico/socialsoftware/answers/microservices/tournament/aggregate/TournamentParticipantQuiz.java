package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantQuizDto;

@Entity
public class TournamentParticipantQuiz {
    @Id
    @GeneratedValue
    private Long id;
    private Integer participantQuizAggregateId;
    private Integer participantQuizVersion;
    private boolean participantQuizAnswered;
    private Integer participantQuizNumberOfAnswered;
    private Integer participantQuizNumberOfCorrect;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentparticipantquiz")
    private TournamentParticipant tournamentParticipant;
    private AggregateState quizState;
    @OneToOne
    private Tournament tournament;

    public TournamentParticipantQuiz() {

    }

    public TournamentParticipantQuiz(QuizDto quizDto) {
        setParticipantQuizAggregateId(quizDto.getAggregateId());
        setParticipantQuizVersion(quizDto.getVersion());
        setQuizState(quizDto.getState());
    }

    public TournamentParticipantQuiz(TournamentParticipantQuiz other) {
        setParticipantQuizVersion(other.getParticipantQuizVersion());
        setParticipantQuizAnswered(other.getParticipantQuizAnswered());
        setParticipantQuizNumberOfAnswered(other.getParticipantQuizNumberOfAnswered());
        setParticipantQuizNumberOfCorrect(other.getParticipantQuizNumberOfCorrect());
        setTournamentParticipant(new TournamentParticipant(other.getTournamentParticipant()));
        setQuizState(other.getQuizState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getParticipantQuizAggregateId() {
        return participantQuizAggregateId;
    }

    public void setParticipantQuizAggregateId(Integer participantQuizAggregateId) {
        this.participantQuizAggregateId = participantQuizAggregateId;
    }

    public Integer getParticipantQuizVersion() {
        return participantQuizVersion;
    }

    public void setParticipantQuizVersion(Integer participantQuizVersion) {
        this.participantQuizVersion = participantQuizVersion;
    }

    public boolean getParticipantQuizAnswered() {
        return participantQuizAnswered;
    }

    public void setParticipantQuizAnswered(boolean participantQuizAnswered) {
        this.participantQuizAnswered = participantQuizAnswered;
    }

    public Integer getParticipantQuizNumberOfAnswered() {
        return participantQuizNumberOfAnswered;
    }

    public void setParticipantQuizNumberOfAnswered(Integer participantQuizNumberOfAnswered) {
        this.participantQuizNumberOfAnswered = participantQuizNumberOfAnswered;
    }

    public Integer getParticipantQuizNumberOfCorrect() {
        return participantQuizNumberOfCorrect;
    }

    public void setParticipantQuizNumberOfCorrect(Integer participantQuizNumberOfCorrect) {
        this.participantQuizNumberOfCorrect = participantQuizNumberOfCorrect;
    }

    public TournamentParticipant getTournamentParticipant() {
        return tournamentParticipant;
    }

    public void setTournamentParticipant(TournamentParticipant tournamentParticipant) {
        this.tournamentParticipant = tournamentParticipant;
        if (this.tournamentParticipant != null) {
            this.tournamentParticipant.setParticipantQuiz(this);
        }
    }

    public AggregateState getQuizState() {
        return quizState;
    }

    public void setQuizState(AggregateState quizState) {
        this.quizState = quizState;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }



    public TournamentParticipantQuizDto buildDto() {
        TournamentParticipantQuizDto dto = new TournamentParticipantQuizDto();
        dto.setAggregateId(getParticipantQuizAggregateId());
        dto.setVersion(getParticipantQuizVersion());
        dto.setParticipantQuizAnswered(getParticipantQuizAnswered());
        dto.setParticipantQuizNumberOfAnswered(getParticipantQuizNumberOfAnswered());
        dto.setParticipantQuizNumberOfCorrect(getParticipantQuizNumberOfCorrect());
        dto.setTournamentParticipant(getTournamentParticipant() != null ? new TournamentParticipantDto(getTournamentParticipant()) : null);
        dto.setState(getQuizState());
        return dto;
    }
}