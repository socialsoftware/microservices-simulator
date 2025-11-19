package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

@Entity
public class TournamentParticipantQuiz {
    @Id
    @GeneratedValue
    private Long id;
    private Integer participantQuizAggregateId;
    private Integer participantQuizVersion;
    private Boolean participantQuizAnswered;
    private Integer participantQuizNumberOfAnswered;
    private Integer participantQuizNumberOfCorrect;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentparticipantquiz")
    private TournamentParticipant tournamentParticipant;
    @OneToOne
    private Tournament tournament;

    public TournamentParticipantQuiz() {

    }

    public TournamentParticipantQuiz(QuizDto quizDto) {
        setParticipantQuizAggregateId(quizDto.getAggregateId());
    }

    public TournamentParticipantQuiz(TournamentParticipantQuiz other) {
        setParticipantQuizVersion(other.getParticipantQuizVersion());
        setParticipantQuizAnswered(other.getParticipantQuizAnswered());
        setParticipantQuizNumberOfAnswered(other.getParticipantQuizNumberOfAnswered());
        setParticipantQuizNumberOfCorrect(other.getParticipantQuizNumberOfCorrect());
        setTournamentParticipant(new TournamentParticipant(other.getTournamentParticipant()));
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

    public Boolean getParticipantQuizAnswered() {
        return participantQuizAnswered;
    }

    public void setParticipantQuizAnswered(Boolean participantQuizAnswered) {
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

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }


    public QuizDto buildDto() {
        QuizDto dto = new QuizDto();
        dto.setAggregateId(getParticipantQuizAggregateId());
        return dto;
    }
}