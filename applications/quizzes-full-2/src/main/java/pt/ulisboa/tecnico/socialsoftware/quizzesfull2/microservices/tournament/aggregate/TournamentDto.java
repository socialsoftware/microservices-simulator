package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TournamentDto {
    private Integer aggregateId;
    private Long version;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    private Integer executionAggregateId;
    private Long executionVersion;
    private Integer courseAggregateId;
    private Integer creatorAggregateId;
    private String creatorName;
    private String creatorUsername;
    private Long creatorVersion;
    private Integer quizAggregateId;
    private Long quizVersion;
    private List<TournamentParticipantDto> participants = new ArrayList<>();
    private List<TournamentTopicDto> topics = new ArrayList<>();

    public TournamentDto() {
    }

    public TournamentDto(Integer aggregateId, Long version, LocalDateTime startTime, LocalDateTime endTime,
                         Integer numberOfQuestions, Boolean cancelled, Integer executionAggregateId,
                         Long executionVersion, Integer courseAggregateId, Integer creatorAggregateId,
                         String creatorName, String creatorUsername, Long creatorVersion, Integer quizAggregateId,
                         Long quizVersion, List<TournamentParticipantDto> participants,
                         List<TournamentTopicDto> topics) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfQuestions = numberOfQuestions;
        this.cancelled = cancelled;
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
        this.courseAggregateId = courseAggregateId;
        this.creatorAggregateId = creatorAggregateId;
        this.creatorName = creatorName;
        this.creatorUsername = creatorUsername;
        this.creatorVersion = creatorVersion;
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
        this.participants = participants;
        this.topics = topics;
    }

    public TournamentDto(Tournament tournament) {
        this.aggregateId = tournament.getAggregateId();
        this.version = tournament.getVersion();
        this.startTime = tournament.getStartTime();
        this.endTime = tournament.getEndTime();
        this.numberOfQuestions = tournament.getNumberOfQuestions();
        this.cancelled = tournament.getCancelled();
        this.executionAggregateId = tournament.getExecution().getExecutionAggregateId();
        this.executionVersion = tournament.getExecution().getExecutionVersion();
        this.courseAggregateId = tournament.getExecution().getCourseAggregateId();
        this.creatorAggregateId = tournament.getCreator().getUserAggregateId();
        this.creatorName = tournament.getCreator().getUserName();
        this.creatorUsername = tournament.getCreator().getUserUsername();
        this.creatorVersion = tournament.getCreator().getUserVersion();
        this.quizAggregateId = tournament.getQuiz().getQuizAggregateId();
        this.quizVersion = tournament.getQuiz().getQuizVersion();
        this.participants = tournament.getParticipants().stream()
                .map(TournamentParticipantDto::new)
                .collect(Collectors.toList());
        this.topics = tournament.getTopics().stream()
                .map(TournamentTopicDto::new)
                .collect(Collectors.toList());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Long getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Long executionVersion) {
        this.executionVersion = executionVersion;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }

    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public Integer getCreatorAggregateId() {
        return creatorAggregateId;
    }

    public void setCreatorAggregateId(Integer creatorAggregateId) {
        this.creatorAggregateId = creatorAggregateId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public Long getCreatorVersion() {
        return creatorVersion;
    }

    public void setCreatorVersion(Long creatorVersion) {
        this.creatorVersion = creatorVersion;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Long getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Long quizVersion) {
        this.quizVersion = quizVersion;
    }

    public List<TournamentParticipantDto> getParticipants() {
        return participants;
    }

    public void setParticipants(List<TournamentParticipantDto> participants) {
        this.participants = participants;
    }

    public List<TournamentTopicDto> getTopics() {
        return topics;
    }

    public void setTopics(List<TournamentTopicDto> topics) {
        this.topics = topics;
    }
}
