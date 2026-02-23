package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentParticipantQuizUpdatedEvent extends Event {
    private Integer quizAggregateId;
    private Integer quizVersion;
    private Boolean participantQuizAnswered;
    private Integer participantQuizNumberOfAnswered;
    private Integer participantQuizNumberOfCorrect;

    public TournamentParticipantQuizUpdatedEvent() {
        super();
    }

    public TournamentParticipantQuizUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentParticipantQuizUpdatedEvent(Integer aggregateId, Integer quizAggregateId, Integer quizVersion, Boolean participantQuizAnswered, Integer participantQuizNumberOfAnswered, Integer participantQuizNumberOfCorrect) {
        super(aggregateId);
        setQuizAggregateId(quizAggregateId);
        setQuizVersion(quizVersion);
        setParticipantQuizAnswered(participantQuizAnswered);
        setParticipantQuizNumberOfAnswered(participantQuizNumberOfAnswered);
        setParticipantQuizNumberOfCorrect(participantQuizNumberOfCorrect);
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Integer getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Integer quizVersion) {
        this.quizVersion = quizVersion;
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

}