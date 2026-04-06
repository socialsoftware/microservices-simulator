package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class TournamentParticipantQuizUpdatedEvent extends Event {
    @Column(name = "tournament_participant_quiz_updated_event_quiz_aggregate_id")
    private Integer quizAggregateId;
    @Column(name = "tournament_participant_quiz_updated_event_quiz_version")
    private Integer quizVersion;
    @Column(name = "tournament_participant_quiz_updated_event_participant__d87b71ca")
    private Boolean participantQuizAnswered;
    @Column(name = "tournament_participant_quiz_updated_event_participant__264546fc")
    private Integer participantQuizNumberOfAnswered;
    @Column(name = "tournament_participant_quiz_updated_event_participant__2a9c766b")
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