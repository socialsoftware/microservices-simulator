package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;
import jakarta.persistence.Column;

@Entity
public class TournamentParticipantUpdatedEvent extends Event {
    @Column(name = "tournament_participant_updated_event_executionuser_aggregate_id")
    private Integer executionuserAggregateId;
    @Column(name = "tournament_participant_updated_event_executionuser_version")
    private Integer executionuserVersion;
    @Column(name = "tournament_participant_updated_event_participant_enroll_time")
    private LocalDateTime participantEnrollTime;

    public TournamentParticipantUpdatedEvent() {
        super();
    }

    public TournamentParticipantUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentParticipantUpdatedEvent(Integer aggregateId, Integer executionuserAggregateId, Integer executionuserVersion, LocalDateTime participantEnrollTime) {
        super(aggregateId);
        setExecutionuserAggregateId(executionuserAggregateId);
        setExecutionuserVersion(executionuserVersion);
        setParticipantEnrollTime(participantEnrollTime);
    }

    public Integer getExecutionuserAggregateId() {
        return executionuserAggregateId;
    }

    public void setExecutionuserAggregateId(Integer executionuserAggregateId) {
        this.executionuserAggregateId = executionuserAggregateId;
    }

    public Integer getExecutionuserVersion() {
        return executionuserVersion;
    }

    public void setExecutionuserVersion(Integer executionuserVersion) {
        this.executionuserVersion = executionuserVersion;
    }

    public LocalDateTime getParticipantEnrollTime() {
        return participantEnrollTime;
    }

    public void setParticipantEnrollTime(LocalDateTime participantEnrollTime) {
        this.participantEnrollTime = participantEnrollTime;
    }

}