package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;

public class TournamentDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    private Integer creatorAggregateId;
    private Set<TournamentParticipant> participants;
    private Integer executionAggregateId;
    private Set<TournamentTopic> topics;
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
        this.cancelled = tournament.isCancelled();
        this.creatorAggregateId = tournament.getCreator() != null ? tournament.getCreator().getAggregateId() : null;
        this.participants = tournament.getParticipants();
        this.executionAggregateId = tournament.getExecution() != null ? tournament.getExecution().getAggregateId() : null;
        this.topics = tournament.getTopics();
        this.quizAggregateId = tournament.getQuiz() != null ? tournament.getQuiz().getAggregateId() : null;
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

    public Boolean isCancelled() {
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

    public Set<TournamentParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<TournamentParticipant> participants) {
        this.participants = participants;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Set<TournamentTopic> getTopics() {
        return topics;
    }

    public void setTopics(Set<TournamentTopic> topics) {
        this.topics = topics;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }
}