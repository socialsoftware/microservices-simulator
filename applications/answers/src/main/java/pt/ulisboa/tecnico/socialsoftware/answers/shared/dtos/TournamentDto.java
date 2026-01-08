package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;

public class TournamentDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    private Integer creatorAggregateId;
    private Set<Integer> participantsAggregateIds;
    private Integer executionAggregateId;
    private Set<Integer> topicsAggregateIds;
    private Integer quizAggregateId;

    public TournamentDto() {
    }

    public TournamentDto(Tournament tournament) {
        this.aggregateId = tournament.getAggregateId();
        this.version = tournament.getVersion();
        this.state = tournament.getState();
        this.startTime = tournament.getStartTime();
        this.endTime = tournament.getEndTime();
        this.numberOfQuestions = tournament.getNumberOfQuestions();
        this.cancelled = tournament.getCancelled();
        this.creatorAggregateId = tournament.getCreator() != null ? tournament.getCreator().getCreatorAggregateId() : null;
        this.participantsAggregateIds = tournament.getParticipants() != null ? tournament.getParticipants().stream().map(item -> item.getParticipantAggregateId()).collect(Collectors.toSet()) : null;
        this.executionAggregateId = tournament.getExecution() != null ? tournament.getExecution().getExecutionAggregateId() : null;
        this.topicsAggregateIds = tournament.getTopics() != null ? tournament.getTopics().stream().map(item -> item.getTopicAggregateId()).collect(Collectors.toSet()) : null;
        this.quizAggregateId = tournament.getQuiz() != null ? tournament.getQuiz().getQuizAggregateId() : null;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Integer getCreatorAggregateId() {
        return creatorAggregateId;
    }

    public void setCreatorAggregateId(Integer creatorAggregateId) {
        this.creatorAggregateId = creatorAggregateId;
    }

    public Set<Integer> getParticipantsAggregateIds() {
        return participantsAggregateIds;
    }

    public void setParticipantsAggregateIds(Set<Integer> participantsAggregateIds) {
        this.participantsAggregateIds = participantsAggregateIds;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Set<Integer> getTopicsAggregateIds() {
        return topicsAggregateIds;
    }

    public void setTopicsAggregateIds(Set<Integer> topicsAggregateIds) {
        this.topicsAggregateIds = topicsAggregateIds;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }
}