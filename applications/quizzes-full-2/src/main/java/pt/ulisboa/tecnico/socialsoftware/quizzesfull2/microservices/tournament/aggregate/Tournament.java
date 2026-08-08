package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain.QuizzesFull2DomainConstants;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "tournaments")
public abstract class Tournament extends Aggregate {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    private LocalDateTime lastModifiedTime;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "tournament")
    private TournamentExecution execution;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "tournament")
    private TournamentCreator creator;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "tournament")
    private TournamentQuiz quiz;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentParticipant> participants = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TournamentTopic> topics = new ArrayList<>();

    public Tournament() {
    }

    public Tournament(Integer aggregateId, Integer executionAggregateId, Long executionVersion,
                      Integer courseAggregateId, Integer creatorAggregateId, String creatorName,
                      String creatorUsername, Long creatorVersion, Integer quizAggregateId, Long quizVersion,
                      LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfQuestions = numberOfQuestions;
        this.cancelled = false;
        this.lastModifiedTime = DateHandler.now();
        setExecution(new TournamentExecution(executionAggregateId, executionVersion, courseAggregateId));
        setCreator(new TournamentCreator(creatorAggregateId, creatorName, creatorUsername, creatorVersion));
        setQuiz(new TournamentQuiz(quizAggregateId, quizVersion));
    }

    public Tournament(Tournament other) {
        super(other);
        this.startTime = other.getStartTime();
        this.endTime = other.getEndTime();
        this.numberOfQuestions = other.getNumberOfQuestions();
        this.cancelled = other.getCancelled();
        this.lastModifiedTime = other.getLastModifiedTime();
        setExecution(new TournamentExecution(other.getExecution()));
        setCreator(new TournamentCreator(other.getCreator()));
        setQuiz(new TournamentQuiz(other.getQuiz()));
        this.participants = other.getParticipants().stream()
                .map(TournamentParticipant::new)
                .collect(Collectors.toList());
        this.topics = other.getTopics().stream()
                .map(TournamentTopic::new)
                .collect(Collectors.toList());
    }

    @Override
    public void verifyInvariants() {
        startBeforeEndTime();
        uniqueAsParticipant();
        enrollUntilStartTime();
        answerBeforeStart();
        creatorParticipantConsistency();
        creatorIsNotAnonymous();
        topicCourseExecution();
        deletedTournamentHasNoParticipants();
        fieldsFinalAfterStart();
        cancelledTournamentIsFrozen();
    }

    private void startBeforeEndTime() {
        if (!this.startTime.isBefore(this.endTime)) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOURNAMENT_START_BEFORE_END_TIME);
        }
    }

    private void uniqueAsParticipant() {
        if (participantAggregateIds().size() != this.participants.size()) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOURNAMENT_UNIQUE_AS_PARTICIPANT);
        }
    }

    private void enrollUntilStartTime() {
        boolean allEnrolledInTime = this.participants.stream()
                .allMatch(participant -> participant.getEnrollTime().isBefore(this.startTime));
        if (!allEnrolledInTime) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOURNAMENT_ENROLL_UNTIL_START_TIME);
        }
    }

    // A participant who has not answered yet carries no firstAnswerTime, so no instant is constrained.
    private void answerBeforeStart() {
        boolean allAnsweredAfterStart = this.participants.stream()
                .map(participant -> participant.getQuizAnswer().getFirstAnswerTime())
                .filter(Objects::nonNull)
                .noneMatch(firstAnswerTime -> firstAnswerTime.isBefore(this.startTime));
        if (!allAnsweredAfterStart) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOURNAMENT_ANSWER_BEFORE_START);
        }
    }

    private void creatorParticipantConsistency() {
        boolean allConsistent = this.participants.stream()
                .filter(participant -> Objects.equals(participant.getUserAggregateId(),
                        this.creator.getUserAggregateId()))
                .allMatch(participant -> Objects.equals(participant.getUserName(), this.creator.getUserName())
                        && Objects.equals(participant.getUserUsername(), this.creator.getUserUsername())
                        && Objects.equals(participant.getUserVersion(), this.creator.getUserVersion()));
        if (!allConsistent) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY);
        }
    }

    private void creatorIsNotAnonymous() {
        boolean anonymous = QuizzesFull2DomainConstants.ANONYMOUS.equals(this.creator.getUserName())
                || QuizzesFull2DomainConstants.ANONYMOUS.equals(this.creator.getUserUsername());
        if (anonymous) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.CREATOR_IS_NOT_ANONYMOUS);
        }
    }

    private void topicCourseExecution() {
        boolean allInCourse = this.topics.stream()
                .allMatch(topic -> Objects.equals(topic.getCourseAggregateId(),
                        this.execution.getCourseAggregateId()));
        if (!allInCourse) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOPIC_COURSE_EXECUTION);
        }
    }

    private void deletedTournamentHasNoParticipants() {
        if (getState() == AggregateState.DELETED && !this.participants.isEmpty()) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOURNAMENT_DELETE);
        }
    }

    private void fieldsFinalAfterStart() {
        Tournament prev = (Tournament) getPrev();
        if (prev == null || !this.lastModifiedTime.isAfter(prev.getStartTime())) {
            return;
        }
        if (!scheduleUnchangedFrom(prev)) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOURNAMENT_FINAL_AFTER_START);
        }
    }

    private void cancelledTournamentIsFrozen() {
        Tournament prev = (Tournament) getPrev();
        if (prev == null || !Boolean.TRUE.equals(prev.getCancelled())) {
            return;
        }
        if (!scheduleUnchangedFrom(prev) || !participantAggregateIds().equals(prev.participantAggregateIds())) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOURNAMENT_IS_CANCELED);
        }
    }

    // Membership, not full equality: what both freezes protect is which topics the tournament draws
    // from and which students take part. The cached name and version of a TournamentTopic or a
    // TournamentParticipant are refreshed by the UpdateTopicEvent and UpdateStudentNameEvent handlers,
    // which must keep working after the tournament starts or is cancelled.
    private boolean scheduleUnchangedFrom(Tournament prev) {
        return Objects.equals(this.startTime, prev.getStartTime())
                && Objects.equals(this.endTime, prev.getEndTime())
                && Objects.equals(this.numberOfQuestions, prev.getNumberOfQuestions())
                && Objects.equals(this.cancelled, prev.getCancelled())
                && topicAggregateIds().equals(prev.topicAggregateIds());
    }

    private Set<Integer> topicAggregateIds() {
        return this.topics.stream()
                .map(TournamentTopic::getTopicAggregateId)
                .collect(Collectors.toSet());
    }

    private Set<Integer> participantAggregateIds() {
        return this.participants.stream()
                .map(TournamentParticipant::getUserAggregateId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        stampLastModifiedTime();
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        stampLastModifiedTime();
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
        stampLastModifiedTime();
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
        stampLastModifiedTime();
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    // TOURNAMENT_FINAL_AFTER_START compares this stamp against prev.startTime, so the clock is read
    // here and never inside verifyInvariants().
    private void stampLastModifiedTime() {
        this.lastModifiedTime = DateHandler.now();
    }

    public TournamentExecution getExecution() {
        return execution;
    }

    // No setter: the Execution reference is immutable (TOURNAMENT_COURSE_EXECUTION_IS_FINAL). Both
    // constructors install the snapshot through here so the back-reference mappedBy = "tournament"
    // resolves is always wired before the entity is persisted. Same for the creator and quiz below.
    private void setExecution(TournamentExecution execution) {
        this.execution = execution;
        execution.setTournament(this);
    }

    public TournamentCreator getCreator() {
        return creator;
    }

    private void setCreator(TournamentCreator creator) {
        this.creator = creator;
        creator.setTournament(this);
    }

    public TournamentQuiz getQuiz() {
        return quiz;
    }

    private void setQuiz(TournamentQuiz quiz) {
        this.quiz = quiz;
        quiz.setTournament(this);
    }

    public List<TournamentParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<TournamentParticipant> participants) {
        this.participants = participants;
    }

    public void addParticipant(TournamentParticipant participant) {
        this.participants.add(participant);
    }

    public void removeParticipant(Integer userAggregateId) {
        this.participants.removeIf(participant -> userAggregateId.equals(participant.getUserAggregateId()));
    }

    public List<TournamentTopic> getTopics() {
        return topics;
    }

    public void setTopics(List<TournamentTopic> topics) {
        this.topics = topics;
        stampLastModifiedTime();
    }

    public void addTopic(TournamentTopic topic) {
        this.topics.add(topic);
        stampLastModifiedTime();
    }

    public void removeTopic(Integer topicAggregateId) {
        this.topics.removeIf(topic -> topicAggregateId.equals(topic.getTopicAggregateId()));
        stampLastModifiedTime();
    }
}
