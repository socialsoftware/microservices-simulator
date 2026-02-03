package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import java.time.LocalDateTime;

@Entity
public class TournamentParticipantUpdatedEvent extends Event {
    private Integer executionuserAggregateId;
    private Integer executionuserVersion;
    private String participantName;
    private String participantUsername;
    private LocalDateTime participantEnrollTime;

    public TournamentParticipantUpdatedEvent() {
        super();
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