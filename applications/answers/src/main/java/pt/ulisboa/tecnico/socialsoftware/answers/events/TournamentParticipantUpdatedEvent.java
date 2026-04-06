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
    @Column(name = "tournament_participant_updated_event_participant_name")
    private String participantName;
    @Column(name = "tournament_participant_updated_event_participant_username")
    private String participantUsername;
    @Column(name = "tournament_participant_updated_event_participant_enroll_time")
    private LocalDateTime participantEnrollTime;

    public TournamentParticipantUpdatedEvent() {
        super();
    }

    public TournamentParticipantUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TournamentParticipantUpdatedEvent(Integer aggregateId, Integer executionuserAggregateId, Integer executionuserVersion, String participantName, String participantUsername, LocalDateTime participantEnrollTime) {
        super(aggregateId);
        setExecutionuserAggregateId(executionuserAggregateId);
        setExecutionuserVersion(executionuserVersion);
        setParticipantName(participantName);
        setParticipantUsername(participantUsername);
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

}