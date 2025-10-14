package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

@Entity
public class TournamentParticipantQuizAnswer {
    @Id
    @GeneratedValue
    private Long id;
    private Integer participantQuizAnswerAggregateId;
    private Integer participantQuizAnswerVersion;
    private Boolean participantQuizAnswerAnswered;
    private Integer participantQuizAnswerNumberOfAnswered;
    private Integer participantQuizAnswerNumberOfCorrect;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentparticipantquizanswer")
    private TournamentParticipant tournamentParticipant;
    @OneToOne
    private Tournament tournament;

    public TournamentParticipantQuizAnswer() {
    }

    public TournamentParticipantQuizAnswer(QuizDto quizDto) {
        setParticipantQuizAnswerAggregateId(quizDto.getAggregateId());
    }

    public TournamentParticipantQuizAnswer(TournamentParticipantQuizAnswer other) {
        setParticipantQuizAnswerAggregateId(other.getParticipantQuizAnswerAggregateId());
        setParticipantQuizAnswerVersion(other.getParticipantQuizAnswerVersion());
        setParticipantQuizAnswerAnswered(other.getParticipantQuizAnswerAnswered());
        setParticipantQuizAnswerNumberOfAnswered(other.getParticipantQuizAnswerNumberOfAnswered());
        setParticipantQuizAnswerNumberOfCorrect(other.getParticipantQuizAnswerNumberOfCorrect());
        setTournamentParticipant(new TournamentParticipant(other.getTournamentParticipant()));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getParticipantQuizAnswerAggregateId() {
        return participantQuizAnswerAggregateId;
    }

    public void setParticipantQuizAnswerAggregateId(Integer participantQuizAnswerAggregateId) {
        this.participantQuizAnswerAggregateId = participantQuizAnswerAggregateId;
    }

    public Integer getParticipantQuizAnswerVersion() {
        return participantQuizAnswerVersion;
    }

    public void setParticipantQuizAnswerVersion(Integer participantQuizAnswerVersion) {
        this.participantQuizAnswerVersion = participantQuizAnswerVersion;
    }

    public Boolean getParticipantQuizAnswerAnswered() {
        return participantQuizAnswerAnswered;
    }

    public void setParticipantQuizAnswerAnswered(Boolean participantQuizAnswerAnswered) {
        this.participantQuizAnswerAnswered = participantQuizAnswerAnswered;
    }

    public Integer getParticipantQuizAnswerNumberOfAnswered() {
        return participantQuizAnswerNumberOfAnswered;
    }

    public void setParticipantQuizAnswerNumberOfAnswered(Integer participantQuizAnswerNumberOfAnswered) {
        this.participantQuizAnswerNumberOfAnswered = participantQuizAnswerNumberOfAnswered;
    }

    public Integer getParticipantQuizAnswerNumberOfCorrect() {
        return participantQuizAnswerNumberOfCorrect;
    }

    public void setParticipantQuizAnswerNumberOfCorrect(Integer participantQuizAnswerNumberOfCorrect) {
        this.participantQuizAnswerNumberOfCorrect = participantQuizAnswerNumberOfCorrect;
    }

    public TournamentParticipant getTournamentParticipant() {
        return tournamentParticipant;
    }

    public void setTournamentParticipant(TournamentParticipant tournamentParticipant) {
        this.tournamentParticipant = tournamentParticipant;
        if (this.tournamentParticipant != null) {
            this.tournamentParticipant.setTournamentParticipantQuizAnswer(this);
        }
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

}