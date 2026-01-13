package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentTopic;

public class TournamentDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    private TournamentCreatorDto creator;
    private Set<TournamentParticipantDto> participants;
    private TournamentExecutionDto execution;
    private Set<TournamentTopicDto> topics;
    private TournamentQuizDto quiz;

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
        this.creator = tournament.getCreator() != null ? new TournamentCreatorDto(tournament.getCreator()) : null;
        this.participants = tournament.getParticipants() != null ? tournament.getParticipants().stream().map(TournamentParticipant::buildDto).collect(Collectors.toSet()) : null;
        this.execution = tournament.getExecution() != null ? new TournamentExecutionDto(tournament.getExecution()) : null;
        this.topics = tournament.getTopics() != null ? tournament.getTopics().stream().map(TournamentTopic::buildDto).collect(Collectors.toSet()) : null;
        this.quiz = tournament.getQuiz() != null ? new TournamentQuizDto(tournament.getQuiz()) : null;
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

    public TournamentCreatorDto getCreator() {
        return creator;
    }

    public void setCreator(TournamentCreatorDto creator) {
        this.creator = creator;
    }

    public Set<TournamentParticipantDto> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<TournamentParticipantDto> participants) {
        this.participants = participants;
    }

    public TournamentExecutionDto getExecution() {
        return execution;
    }

    public void setExecution(TournamentExecutionDto execution) {
        this.execution = execution;
    }

    public Set<TournamentTopicDto> getTopics() {
        return topics;
    }

    public void setTopics(Set<TournamentTopicDto> topics) {
        this.topics = topics;
    }

    public TournamentQuizDto getQuiz() {
        return quiz;
    }

    public void setQuiz(TournamentQuizDto quiz) {
        this.quiz = quiz;
    }
}