package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipantQuiz;

public class TournamentParticipantQuizDto implements Serializable {
    private Integer participantQuizVersion;
    private Boolean participantQuizAnswered;
    private Integer participantQuizNumberOfAnswered;
    private Integer participantQuizNumberOfCorrect;
    private TournamentParticipantDto tournamentParticipant;
    private Integer aggregateId;

    public TournamentParticipantQuizDto() {
    }

    public TournamentParticipantQuizDto(TournamentParticipantQuiz tournamentParticipantQuiz) {
        this.participantQuizVersion = tournamentParticipantQuiz.getParticipantQuizVersion();
        this.participantQuizAnswered = tournamentParticipantQuiz.getParticipantQuizAnswered();
        this.participantQuizNumberOfAnswered = tournamentParticipantQuiz.getParticipantQuizNumberOfAnswered();
        this.participantQuizNumberOfCorrect = tournamentParticipantQuiz.getParticipantQuizNumberOfCorrect();
        this.tournamentParticipant = tournamentParticipantQuiz.getTournamentParticipant() != null ? new TournamentParticipantDto(tournamentParticipantQuiz.getTournamentParticipant()) : null;
        this.aggregateId = tournamentParticipantQuiz.getParticipantQuizAggregateId();
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

    public TournamentParticipantDto getTournamentParticipant() {
        return tournamentParticipant;
    }

    public void setTournamentParticipant(TournamentParticipantDto tournamentParticipant) {
        this.tournamentParticipant = tournamentParticipant;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }
}