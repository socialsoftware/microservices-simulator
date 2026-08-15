package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TournamentDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;

    private Integer executionAggregateId;
    private Long executionVersion;

    private Integer creatorAggregateId;
    private String creatorName;
    private String creatorUsername;
    private Long creatorVersion;

    private Integer quizAggregateId;
    private Long quizVersion;

    private List<Integer> topicIds = new ArrayList<>();
    private List<Integer> participantIds = new ArrayList<>();

    public TournamentDto() {}

    public TournamentDto(Tournament tournament) {
        setAggregateId(tournament.getAggregateId());
        setVersion(tournament.getVersion());
        setState(tournament.getState());
        setStartTime(tournament.getStartTime());
        setEndTime(tournament.getEndTime());
        setNumberOfQuestions(tournament.getNumberOfQuestions());
        setCancelled(tournament.getCancelled());
        setExecutionAggregateId(tournament.getExecutionAggregateId());
        setExecutionVersion(tournament.getExecutionVersion());
        setCreatorAggregateId(tournament.getCreatorAggregateId());
        setCreatorName(tournament.getCreatorName());
        setCreatorUsername(tournament.getCreatorUsername());
        setCreatorVersion(tournament.getCreatorVersion());
        setQuizAggregateId(tournament.getQuizAggregateId());
        setQuizVersion(tournament.getQuizVersion());
        for (TournamentTopic topic : tournament.getTopics()) {
            this.topicIds.add(topic.getTopicAggregateId());
        }
        for (TournamentParticipant participant : tournament.getParticipants()) {
            this.participantIds.add(participant.getParticipantAggregateId());
        }
    }

    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public AggregateState getState() { return state; }
    public void setState(AggregateState state) { this.state = state; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getNumberOfQuestions() { return numberOfQuestions; }
    public void setNumberOfQuestions(Integer numberOfQuestions) { this.numberOfQuestions = numberOfQuestions; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public void setExecutionAggregateId(Integer executionAggregateId) { this.executionAggregateId = executionAggregateId; }

    public Long getExecutionVersion() { return executionVersion; }
    public void setExecutionVersion(Long executionVersion) { this.executionVersion = executionVersion; }

    public Integer getCreatorAggregateId() { return creatorAggregateId; }
    public void setCreatorAggregateId(Integer creatorAggregateId) { this.creatorAggregateId = creatorAggregateId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }

    public Long getCreatorVersion() { return creatorVersion; }
    public void setCreatorVersion(Long creatorVersion) { this.creatorVersion = creatorVersion; }

    public Integer getQuizAggregateId() { return quizAggregateId; }
    public void setQuizAggregateId(Integer quizAggregateId) { this.quizAggregateId = quizAggregateId; }

    public Long getQuizVersion() { return quizVersion; }
    public void setQuizVersion(Long quizVersion) { this.quizVersion = quizVersion; }

    public List<Integer> getTopicIds() { return topicIds; }
    public void setTopicIds(List<Integer> topicIds) { this.topicIds = topicIds; }

    public List<Integer> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<Integer> participantIds) { this.participantIds = participantIds; }
}
