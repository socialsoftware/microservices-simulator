package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe.TournamentSubscribesAnonymizeStudent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe.TournamentSubscribesDeleteCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe.TournamentSubscribesDeleteTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe.TournamentSubscribesDeleteUser;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe.TournamentSubscribesInvalidateQuiz;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe.TournamentSubscribesQuizAnswerQuestionAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe.TournamentSubscribesUpdateStudentName;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe.TournamentSubscribesUpdateTopic;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/*
    INTRA-INVARIANTS:
        TOURNAMENT_START_BEFORE_END_TIME: startTime < endTime
        TOURNAMENT_UNIQUE_AS_PARTICIPANT: all participants have distinct aggregateIds
        TOURNAMENT_ENROLL_UNTIL_START_TIME: ∀p: p.enrollTime < startTime
        TOURNAMENT_FINAL_AFTER_START: once started, startTime/endTime/numberOfQuestions/topics/cancelled are frozen
        TOURNAMENT_IS_CANCELED: once cancelled, all fields and participants are frozen
        TOURNAMENT_DELETE: state==DELETED → participants.isEmpty()
        TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY: creator-as-participant has consistent name/username/version
    INTER-INVARIANTS:
        CREATOR_EXISTS / PARTICIPANT_EXISTS (DeleteUserEvent, UpdateStudentNameEvent, AnonymizeStudentEvent)
        TOPIC_EXISTS (UpdateTopicEvent, DeleteTopicEvent)
        QUIZ_EXISTS (InvalidateQuizEvent)
        COURSE_EXECUTION_EXISTS (DeleteCourseExecutionEvent)
        QUIZ_ANSWER_EXISTS (QuizAnswerQuestionAnswerEvent)
 */
@Entity
public abstract class Tournament extends Aggregate {

    @Column
    private final Integer executionAggregateId;

    @Column
    private Long executionVersion;

    @Column
    private final Integer creatorAggregateId;

    @Column
    private String creatorName;

    @Column
    private String creatorUsername;

    @Column
    private Long creatorVersion;

    @Column
    private final Integer quizAggregateId;

    @Column
    private Long quizVersion;

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column
    private Integer numberOfQuestions;

    @Column
    private Boolean cancelled;

    @Column
    private LocalDateTime lastModifiedTime;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TournamentTopic> topics = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TournamentParticipant> participants = new HashSet<>();

    public Tournament() {
        this.executionAggregateId = null;
        this.creatorAggregateId = null;
        this.quizAggregateId = null;
    }

    public Tournament(Integer aggregateId,
                      Integer executionAggregateId, Long executionVersion,
                      Integer creatorAggregateId, String creatorName, String creatorUsername, Long creatorVersion,
                      Integer quizAggregateId, Long quizVersion,
                      Set<TournamentTopic> topics,
                      LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
        this.creatorAggregateId = creatorAggregateId;
        this.creatorName = creatorName;
        this.creatorUsername = creatorUsername;
        this.creatorVersion = creatorVersion;
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
        this.topics = new HashSet<>(topics);
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfQuestions = numberOfQuestions;
        this.cancelled = false;
        this.lastModifiedTime = LocalDateTime.now();
    }

    public Tournament(Tournament other) {
        super(other);
        this.executionAggregateId = other.getExecutionAggregateId();
        this.executionVersion = other.getExecutionVersion();
        this.creatorAggregateId = other.getCreatorAggregateId();
        this.creatorName = other.getCreatorName();
        this.creatorUsername = other.getCreatorUsername();
        this.creatorVersion = other.getCreatorVersion();
        this.quizAggregateId = other.getQuizAggregateId();
        this.quizVersion = other.getQuizVersion();
        this.startTime = other.getStartTime();
        this.endTime = other.getEndTime();
        this.numberOfQuestions = other.getNumberOfQuestions();
        this.cancelled = other.getCancelled();
        this.lastModifiedTime = other.getLastModifiedTime();
        for (TournamentTopic topic : other.getTopics()) {
            this.topics.add(new TournamentTopic(topic));
        }
        for (TournamentParticipant participant : other.getParticipants()) {
            this.participants.add(new TournamentParticipant(participant));
        }
    }

    private boolean startBeforeEndTime() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return startTime.isBefore(endTime);
    }

    private boolean uniqueParticipants() {
        Set<Integer> ids = participants.stream()
                .map(TournamentParticipant::getParticipantAggregateId)
                .collect(Collectors.toSet());
        return ids.size() == participants.size();
    }

    private boolean enrollUntilStartTime() {
        if (startTime == null) {
            return true;
        }
        for (TournamentParticipant p : participants) {
            if (p.getEnrollTime() != null && !p.getEnrollTime().isBefore(startTime)) {
                return false;
            }
        }
        return true;
    }

    private boolean tournamentFinalAfterStart() {
        if (getPrev() == null || !(getPrev() instanceof Tournament)) {
            return true;
        }
        Tournament prev = (Tournament) getPrev();
        if (prev.getStartTime() == null || lastModifiedTime == null) {
            return true;
        }
        if (lastModifiedTime.isAfter(prev.getStartTime())) {
            Set<Integer> currentTopicIds = topics.stream()
                    .map(TournamentTopic::getTopicAggregateId).collect(Collectors.toSet());
            Set<Integer> prevTopicIds = prev.getTopics().stream()
                    .map(TournamentTopic::getTopicAggregateId).collect(Collectors.toSet());
            return startTime.equals(prev.getStartTime())
                    && endTime.equals(prev.getEndTime())
                    && numberOfQuestions.equals(prev.getNumberOfQuestions())
                    && currentTopicIds.equals(prevTopicIds)
                    && cancelled.equals(prev.getCancelled());
        }
        return true;
    }

    private boolean tournamentIsCanceled() {
        if (getPrev() == null || !(getPrev() instanceof Tournament)) {
            return true;
        }
        Tournament prev = (Tournament) getPrev();
        if (!Boolean.TRUE.equals(prev.getCancelled())) {
            return true;
        }
        Set<Integer> currentTopicIds = topics.stream()
                .map(TournamentTopic::getTopicAggregateId).collect(Collectors.toSet());
        Set<Integer> prevTopicIds = prev.getTopics().stream()
                .map(TournamentTopic::getTopicAggregateId).collect(Collectors.toSet());
        Set<Integer> currentParticipantIds = participants.stream()
                .map(TournamentParticipant::getParticipantAggregateId).collect(Collectors.toSet());
        Set<Integer> prevParticipantIds = prev.getParticipants().stream()
                .map(TournamentParticipant::getParticipantAggregateId).collect(Collectors.toSet());
        return startTime.equals(prev.getStartTime())
                && endTime.equals(prev.getEndTime())
                && numberOfQuestions.equals(prev.getNumberOfQuestions())
                && currentTopicIds.equals(prevTopicIds)
                && cancelled.equals(prev.getCancelled())
                && currentParticipantIds.equals(prevParticipantIds);
    }

    private boolean tournamentDeleteHasNoParticipants() {
        if (getState() == AggregateState.DELETED) {
            return participants.isEmpty();
        }
        return true;
    }

    private boolean creatorParticipantConsistency() {
        for (TournamentParticipant p : participants) {
            if (p.getParticipantAggregateId().equals(creatorAggregateId)) {
                if (!p.getParticipantName().equals(creatorName)
                        || !p.getParticipantUsername().equals(creatorUsername)
                        || !p.getParticipantVersion().equals(creatorVersion)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void verifyInvariants() {
        if (!startBeforeEndTime()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.TOURNAMENT_START_BEFORE_END_TIME);
        }
        if (!uniqueParticipants()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.TOURNAMENT_UNIQUE_AS_PARTICIPANT);
        }
        if (!enrollUntilStartTime()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.TOURNAMENT_ENROLL_UNTIL_START_TIME);
        }
        if (!tournamentFinalAfterStart()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.TOURNAMENT_FINAL_AFTER_START);
        }
        if (!tournamentIsCanceled()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.TOURNAMENT_IS_CANCELED);
        }
        if (!tournamentDeleteHasNoParticipants()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.TOURNAMENT_DELETE);
        }
        if (!creatorParticipantConsistency()) {
            throw new QuizzesFullException(QuizzesFullErrorMessage.TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> subscriptions = new HashSet<>();
        // CREATOR_EXISTS / PARTICIPANT_EXISTS
        subscriptions.add(new TournamentSubscribesDeleteUser(creatorAggregateId, creatorVersion));
        subscriptions.add(new TournamentSubscribesUpdateStudentName(creatorAggregateId, creatorVersion));
        subscriptions.add(new TournamentSubscribesAnonymizeStudent(creatorAggregateId, creatorVersion));
        for (TournamentParticipant p : participants) {
            subscriptions.add(new TournamentSubscribesDeleteUser(p.getParticipantAggregateId(), p.getParticipantVersion()));
            subscriptions.add(new TournamentSubscribesUpdateStudentName(p.getParticipantAggregateId(), p.getParticipantVersion()));
            subscriptions.add(new TournamentSubscribesAnonymizeStudent(p.getParticipantAggregateId(), p.getParticipantVersion()));
        }
        // TOPIC_EXISTS
        for (TournamentTopic topic : topics) {
            subscriptions.add(new TournamentSubscribesUpdateTopic(topic));
            subscriptions.add(new TournamentSubscribesDeleteTopic(topic));
        }
        // COURSE_EXECUTION_EXISTS
        subscriptions.add(new TournamentSubscribesDeleteCourseExecution(this));
        // QUIZ_EXISTS
        subscriptions.add(new TournamentSubscribesInvalidateQuiz(this));
        // QUIZ_ANSWER_EXISTS — only for participants with a known quiz answer ID
        for (TournamentParticipant p : participants) {
            if (p.getQuizAnswer() != null && p.getQuizAnswer().getQuizAnswerAggregateId() != null) {
                subscriptions.add(new TournamentSubscribesQuizAnswerQuestionAnswer(p.getQuizAnswer()));
            }
        }
        return subscriptions;
    }

    public Integer getExecutionAggregateId() { return executionAggregateId; }

    public Long getExecutionVersion() { return executionVersion; }
    public void setExecutionVersion(Long executionVersion) { this.executionVersion = executionVersion; }

    public Integer getCreatorAggregateId() { return creatorAggregateId; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }

    public Long getCreatorVersion() { return creatorVersion; }
    public void setCreatorVersion(Long creatorVersion) { this.creatorVersion = creatorVersion; }

    public Integer getQuizAggregateId() { return quizAggregateId; }

    public Long getQuizVersion() { return quizVersion; }
    public void setQuizVersion(Long quizVersion) { this.quizVersion = quizVersion; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        this.lastModifiedTime = LocalDateTime.now();
    }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.lastModifiedTime = LocalDateTime.now();
    }

    public Integer getNumberOfQuestions() { return numberOfQuestions; }
    public void setNumberOfQuestions(Integer numberOfQuestions) { this.numberOfQuestions = numberOfQuestions; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }

    public Set<TournamentTopic> getTopics() { return topics; }
    public void setTopics(Set<TournamentTopic> topics) {
        this.topics = topics;
        this.lastModifiedTime = LocalDateTime.now();
    }
    public void addTopic(TournamentTopic topic) { this.topics.add(topic); }
    public void removeTopic(TournamentTopic topic) { this.topics.remove(topic); }

    public Set<TournamentParticipant> getParticipants() { return participants; }
    public void setParticipants(Set<TournamentParticipant> participants) { this.participants = participants; }
    public void addParticipant(TournamentParticipant participant) { this.participants.add(participant); }
    public void removeParticipant(TournamentParticipant participant) { this.participants.remove(participant); }
}
