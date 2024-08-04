package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_MERGE_FAILURE;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.CANNOT_ADD_PARTICIPANT;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.CANNOT_DELETE_TOURNAMENT;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.CANNOT_UPDATE_TOURNAMENT;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.INVARIANT_BREAK;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.MergeableAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesAnonymizeStudent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesAnswerQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesDeleteCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesDeleteTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesDisenrollStudentFromCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesInvalidateQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesUpdateStudentName;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesUpdateTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils.DateHandler;

/* each version of the tournament is a new instance of the tournament*/
/*
    INTRA-INVARIANTS:
    Intra-Invariants (Causal Consistency check on version merge) (Eventual Consistency check on set to APPROVED) (Apply to ACTIVE states)
        CREATOR_IS_FINAL
        COURSE_EXECUTION_IS_FINAL
        QUIZ_IS_FINAL
        START_BEFORE_END_TIME
        UNIQUE_AS_PARTICIPANT
        ENROLL_UNTIL_START_TIME
        ANSWER_BEFORE_START
        FINAL_AFTER_START
        LEAVE_TOURNAMENT
        AFTER_END
        IS_CANCELED
        DELETE
        CREATOR_PARTICIPANT_CONSISTENCY
    INTER-INVARIANTS:
        NUMBER_OF_QUESTIONS
        QUIZ_TOPICS
        START_TIME_AVAILABLE_DATE
        END_TIME_CONCLUSION_DATE
        CREATOR_COURSE_EXECUTION
        PARTICIPANT_COURSE_EXECUTION
        QUIZ_COURSE_EXECUTION
        TOPIC_COURSE_EXECUTION
        QUIZ_QUIZ_ANSWER
        CREATOR_STUDENT
        PARTICIPANT_STUDENT
        NUMBER_OF_ANSWERED
        NUMBER_OF_CORRECT
        INACTIVE_PROPAGATION ????
        CREATOR_EXISTS
        COURSE_EXECUTION_EXISTS
        PARTICIPANT_EXISTS
        TOPIC_EXISTS
        QUIZ_EXISTS
        QUIZ_ANSWER_EXISTS
 */
@Entity
public abstract class Tournament extends Aggregate implements MergeableAggregate {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private boolean cancelled;
    /*
    CREATOR_IS_FINAL
		final this.creator.id
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentCreator tournamentCreator;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "tournament")
    private Set<TournamentParticipant> tournamentParticipants = new HashSet<>();
    /*
    COURSE_EXECUTION_IS_FINAL
		final this.courseExecution.id
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentCourseExecution tournamentCourseExecution;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "tournament")
    private Set<TournamentTopic> tournamentTopics = new HashSet<>();
    /*
    QUIZ_IS_FINAL
		final this.tournamentQuiz.id
     */
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentQuiz tournamentQuiz;

    public Tournament() {
    }

    public Tournament(Integer aggregateId, TournamentDto tournamentDto, UserDto creatorDto,
                      CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos, QuizDto quizDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setStartTime(DateHandler.toLocalDateTime(tournamentDto.getStartTime()));
        setEndTime(DateHandler.toLocalDateTime(tournamentDto.getEndTime()));
        setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
        setCancelled(tournamentDto.isCancelled());

        setTournamentCreator(new TournamentCreator(creatorDto.getAggregateId(), creatorDto.getName(), creatorDto.getUsername(), creatorDto.getVersion()));
        setTournamentCourseExecution(new TournamentCourseExecution(courseExecutionDto));

        Set<TournamentTopic> tournamentTopics = topicDtos.stream()
                .map(TournamentTopic::new)
                .collect(Collectors.toSet());
        setTournamentTopics(tournamentTopics);

        setTournamentQuiz(new TournamentQuiz(quizDto.getAggregateId(), quizDto.getVersion()));
    }

    /* used to update the tournament by creating new versions */
    public Tournament(Tournament other) {
        super(other);
        setStartTime(other.getStartTime());
        setEndTime(other.getEndTime());
        setNumberOfQuestions(other.getNumberOfQuestions());
        setCancelled(other.isCancelled());

        setTournamentCreator(new TournamentCreator(other.getTournamentCreator()));
        setTournamentCourseExecution(new TournamentCourseExecution(other.getTournamentCourseExecution()));
        setTournamentTopics(other.getTournamentTopics().stream().map(TournamentTopic::new).collect(Collectors.toSet()));

        setTournamentQuiz(new TournamentQuiz(other.getTournamentQuiz()));

        setTournamentParticipants(other.getTournamentParticipants().stream().map(TournamentParticipant::new).collect(Collectors.toSet()));
    }

    @Override
    public void remove() {
        /*
        DELETE
		    this.state == DELETED => this.participants.empty
         */
        if (getTournamentParticipants().size() > 0) {
            throw new TutorException(CANNOT_DELETE_TOURNAMENT, getAggregateId());
        }
        super.remove();
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantCourseExecutionExists(eventSubscriptions);
            interInvariantCreatorExists(eventSubscriptions);
            interInvariantParticipantExists(eventSubscriptions);
            interInvariantQuizAnswersExist(eventSubscriptions);
            interInvariantTopicsExist(eventSubscriptions);
            interInvariantQuizExists(eventSubscriptions);
        }
        return eventSubscriptions;
    }

    private void interInvariantCourseExecutionExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TournamentSubscribesDeleteCourseExecution(this.getTournamentCourseExecution()));
    }

    private void interInvariantCreatorExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TournamentSubscribesDisenrollStudentFromCourseExecution(this));
        eventSubscriptions.add(new TournamentSubscribesAnonymizeStudent(this));
        eventSubscriptions.add(new TournamentSubscribesUpdateStudentName(this));
    }

    private void interInvariantParticipantExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TournamentSubscribesDisenrollStudentFromCourseExecution(this));
        eventSubscriptions.add(new TournamentSubscribesAnonymizeStudent(this));
        eventSubscriptions.add(new TournamentSubscribesUpdateStudentName(this));
    }

    private void interInvariantQuizAnswersExist(Set<EventSubscription> eventSubscriptions) {
        for (TournamentParticipant tournamentParticipant: this.tournamentParticipants) {
            if (tournamentParticipant.getParticipantAnswer().getQuizAnswerAggregateId() != null) {
                eventSubscriptions.add(new TournamentSubscribesAnswerQuestion(tournamentParticipant));
            }
        }
    }

    private void interInvariantTopicsExist(Set<EventSubscription> eventSubscriptions) {
        for (TournamentTopic tournamentTopic: this.tournamentTopics) {
            eventSubscriptions.add(new TournamentSubscribesDeleteTopic(tournamentTopic));
            eventSubscriptions.add(new TournamentSubscribesUpdateTopic(tournamentTopic));
        }
    }

    private void interInvariantQuizExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TournamentSubscribesInvalidateQuiz(this.getTournamentQuiz()));
    }

    /* ----------------------------------------- INTRA-AGGREGATE INVARIANTS ----------------------------------------- */

    /*
    START_BEFORE_END_TIME
		this.startTime < this.endTime
     */
    public boolean invariantStartTimeBeforeEndTime() {
        return this.startTime.isBefore(this.endTime);
    }

    /*
    UNIQUE_AS_PARTICIPANT
		p1, p2: this.participants | p1.id != p2.id
     */
    public boolean invariantUniqueParticipant() {
        return this.tournamentParticipants.size()
                ==
                this.tournamentParticipants.stream()
                .map(TournamentParticipant::getParticipantAggregateId)
                .distinct()
                .count();
    }

    /*
    ENROLL_UNTIL_START_TIME
		p : this.participants | p.enrollTime < this.startTime
     */
    public boolean invariantParticipantsEnrolledBeforeStarTime() {
        for (TournamentParticipant p : this.tournamentParticipants) {
            if (p.getEnrollTime().isAfter(this.startTime)) {
                return false;
            }
        }
        return true;
    }

    /*
    ANSWER_BEFORE_START
		now < this.startTime => p: this.participant | p.answer.isEmpty
     */
    public boolean invariantAnswerBeforeStart() {
        if (DateHandler.now().isBefore(this.startTime)) {
            for (TournamentParticipant t : this.tournamentParticipants) {
                if (t.getParticipantAnswer().getQuizAnswerAggregateId() != null) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
    DELETE
		this.state == DELETED => this.participants.empty
     */
    private boolean invariantDeleteWhenNoParticipants() {
        if (getState() == AggregateState.DELETED) {
            return getTournamentParticipants().size() == 0;
        }
        return true;
    }

    /*
        CREATOR_PARTICIPANT_CONSISTENCY
     */

    private boolean invariantCreatorParticipantConsistency() {
        for (TournamentParticipant participant : this.tournamentParticipants) {
            if (participant.getParticipantAggregateId().equals(this.tournamentCreator.getCreatorAggregateId())) {
                if (!participant.getParticipantVersion().equals(this.tournamentCreator.getCreatorVersion())
                        || !participant.getParticipantName().equals(this.tournamentCreator.getCreatorName())
                        || !participant.getParticipantUsername().equals(this.tournamentCreator.getCreatorUsername())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean invariantCreatorIsNotAnonymous() {
        return !tournamentCreator.getCreatorName().equals("ANONYMOUS") && !tournamentCreator.getCreatorUsername().equals("ANONYMOUS");
    }

    @Override
    public void verifyInvariants() {
        if (!(invariantAnswerBeforeStart()
               && invariantUniqueParticipant()
                && invariantParticipantsEnrolledBeforeStarTime()
                && invariantStartTimeBeforeEndTime()
                && invariantDeleteWhenNoParticipants()
                && invariantCreatorParticipantConsistency())
                && invariantCreatorIsNotAnonymous()) {
            throw new TutorException(INVARIANT_BREAK, getAggregateId());
        }
    }

    public void cancel() {
        this.cancelled = true;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        /*
        FINAL_AFTER_START
		    now > this.startTime => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final canceled
        IS_CANCELED
		    this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
         */
        Tournament prev = (Tournament) getPrev();
        if (prev != null) {
            if ((prev.getStartTime() != null && DateHandler.now().isAfter(prev.getStartTime())) || prev.isCancelled()) {
                throw new TutorException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
            }
        }
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {

        return this.endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        /*
        FINAL_AFTER_START
		    now > this.startTime => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final canceled
        IS_CANCELED
		    this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
         */
        Tournament prev = (Tournament) getPrev();
        if (prev != null) {
            if ((prev.getStartTime() != null && DateHandler.now().isAfter(prev.getStartTime())) || prev.isCancelled()) {
                throw new TutorException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
            }
        }
        this.endTime = endTime;
    }

    public Integer getNumberOfQuestions() {
        return this.numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        /*
        FINAL_AFTER_START
		    now > this.startTime => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final canceled
        IS_CANCELED
		    this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
         */
        Tournament prev = (Tournament) getPrev();
        if (prev != null) {
            if ((prev.getStartTime() != null && DateHandler.now().isAfter(prev.getStartTime())) || prev.isCancelled()) {
                throw new TutorException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
            }
        }
        this.numberOfQuestions = numberOfQuestions;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        /*
        FINAL_AFTER_START
		    now > this.startTime => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final canceled
        IS_CANCELED
		    this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
         */
        Tournament prev = (Tournament) getPrev();
        if (prev != null) {
            if ((prev.getStartTime() != null && DateHandler.now().isAfter(prev.getStartTime())) || prev.isCancelled()) {
                throw new TutorException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
            }
        }
        this.cancelled = cancelled;
    }

    public TournamentCreator getTournamentCreator() {
        return tournamentCreator;
    }

    public void setTournamentCreator(TournamentCreator tournamentCreator) {
        this.tournamentCreator = tournamentCreator;
        tournamentCreator.setTournament(this);
    }

    public Set<TournamentParticipant> getTournamentParticipants() {
        return tournamentParticipants;
    }

    public void setTournamentParticipants(Set<TournamentParticipant> tournamentParticipants) {
        /*
        IS_CANCELED
		    this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
         */
        /*Tournament prev = (Tournament) getPrev();
        if(prev != null && prev.isCancelled()) {
            throw new TutorException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
        }*/
        this.tournamentParticipants = tournamentParticipants;
        this.tournamentParticipants.forEach(tournamentParticipant -> tournamentParticipant.setTournament(this));
    }

    public void addParticipant(TournamentParticipant participant) {
        // INV: tournamentParticipants is final after start time
        Tournament prev = (Tournament) getPrev();
        if (DateHandler.now().isAfter(prev.getStartTime())) {
            throw new TutorException(CANNOT_ADD_PARTICIPANT, getAggregateId());
        }
        /*
        IS_CANCELED
		    this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
         */
        if (prev != null && prev.isCancelled()) {
            throw new TutorException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
        }
        this.tournamentParticipants.add(participant);
        participant.setTournament(this);
    }

    public TournamentCourseExecution getTournamentCourseExecution() {
        return this.tournamentCourseExecution;
    }

    public void setTournamentCourseExecution(TournamentCourseExecution tournamentCourseExecution) {
        this.tournamentCourseExecution = tournamentCourseExecution;
        this.tournamentCourseExecution.setTournament(this);
    }

    public Set<TournamentTopic> getTournamentTopics() {
        return tournamentTopics;
    }

    public void setTournamentTopics(Set<TournamentTopic> topics) {
        /*
        FINAL_AFTER_START
		    now > this.startTime => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final canceled
        IS_CANCELED
		    this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
         */
        Tournament prev = (Tournament) getPrev();
        if (prev != null) {
            if ((prev.getStartTime() != null && DateHandler.now().isAfter(prev.getStartTime())) || prev.isCancelled()) {
                throw new TutorException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
            }
        }

        this.tournamentTopics = topics;
        this.tournamentTopics.forEach(tournamentTopic -> tournamentTopic.setTournament(this));
    }

    public TournamentQuiz getTournamentQuiz() {
        return this.tournamentQuiz;
    }

    public void setTournamentQuiz(TournamentQuiz tournamentQuiz) {
        this.tournamentQuiz = tournamentQuiz;
        this.tournamentQuiz.setTournament(this);
    }

    public TournamentParticipant findParticipant(Integer userAggregateId) {
        return this.tournamentParticipants.stream().filter(p -> p.getParticipantAggregateId().equals(userAggregateId)).findFirst()
                .orElse(null);
    }

    public boolean removeParticipant(TournamentParticipant participant) {
        /*
        LEAVE_TOURNAMENT
		    p: this.participants | p.state == DELETED => p.answer.isEmpty
        AFTER_END
		    now > this.endTime => p: this.participant | final p.answer
		IS_CANCELED
		    this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
         */
        Tournament prev = (Tournament) getPrev();
        if (prev != null) {
            if ((prev.getStartTime() != null && DateHandler.now().isAfter(prev.getStartTime())) || prev.isCancelled()) {
                throw new TutorException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
            }
        }
        return this.tournamentParticipants.remove(participant);
    }

    // this setVersion is special because the quiz is created in the same transaction and we want to have its version upon commit
    @Override
    public void setVersion(Integer version) {
        if (this.tournamentQuiz.getQuizVersion() == null) {
            this.tournamentQuiz.setQuizVersion(version);
        }
        super.setVersion(version);
    }

    public TournamentTopic findTopic(Integer topicAggregateId) {
        return getTournamentTopics().stream()
                .filter(t -> topicAggregateId.equals(t.getTopicAggregateId()))
                .findFirst()
                .orElse(null);
    }

    public void removeTopic(TournamentTopic tournamentTopic) {
        this.tournamentTopics.remove(tournamentTopic);
    }

    @Override
    public Set<String> getMutableFields()  {
        return Set.of("startTime", "endTime", "numberOfQuestions", "tournamentTopics", "tournamentParticipants", "cancelled", "tournamentCourseExecution", "tournamentCreator");
    }

    @Override
    public Set<String[]> getIntentions() {
        return Set.of(
                new String[]{"startTime", "endTime"},
                new String[]{"startTime", "numberOfQuestions"},
                new String[]{"endTime", "numberOfQuestions"},
                new String[]{"numberOfQuestions", "tournamentTopics"});
    }

    @Override
    public Aggregate mergeFields(Set<String> toCommitVersionChangedFields, Aggregate committedVersion, Set<String> committedVersionChangedFields){
        if (!(committedVersion instanceof Tournament)) {
            throw new TutorException(AGGREGATE_MERGE_FAILURE, getAggregateId());
        }

        Tournament committedTournament = (Tournament) committedVersion;

        // merge of creator is built in the participants dont know yet
        mergeCreator(committedTournament, this);
        //mergeCourseExecution(committedTournament, this);
        mergeCourseExecution(committedTournament, this);
        mergeQuiz(committedTournament, this);
        mergeCancelled(toCommitVersionChangedFields, committedTournament, this);
        mergeStartTime(toCommitVersionChangedFields, committedTournament, this);
        mergeEndTime(toCommitVersionChangedFields, committedTournament, this);
        mergeNumberOfQuestions(toCommitVersionChangedFields, committedTournament, this);
        mergeParticipants((Tournament) getPrev(), this, committedTournament, this);
        //mergeTopics((Tournament) getPrev(), this, committedTournament, this);
        mergeTopics(toCommitVersionChangedFields, committedTournament, this);

        return this;
    }

    private void mergeCreator(Tournament committedTournament, Tournament mergedTournament) {
        if (getTournamentCourseExecution().getCourseExecutionVersion() >= committedTournament.getTournamentCourseExecution().getCourseExecutionVersion()) {
            mergedTournament.getTournamentCreator().setCreatorName(getTournamentCreator().getCreatorName());
            mergedTournament.getTournamentCreator().setCreatorUsername(getTournamentCreator().getCreatorUsername());
            mergedTournament.getTournamentCreator().setCreatorVersion(getTournamentCreator().getCreatorVersion());
        } else {
            mergedTournament.getTournamentCreator().setCreatorName(committedTournament.getTournamentCreator().getCreatorName());
            mergedTournament.getTournamentCreator().setCreatorUsername(committedTournament.getTournamentCreator().getCreatorUsername());
            mergedTournament.getTournamentCreator().setCreatorVersion(committedTournament.getTournamentCreator().getCreatorVersion());
        }
    }

    /*private void mergeCourseExecution(Tournament committedTournament, Tournament mergedTournament) {
        if(getCourseExecution().getVersion() >= committedTournament.getCourseExecution().getVersion()) {
            mergedTournament.getCourseExecution().setVersion(getCourseExecution().getVersion());
        } else {
            mergedTournament.getCourseExecution().setVersion(committedTournament.getCourseExecution().getVersion());
        }
    }*/

    private void mergeCourseExecution(Tournament committedTournament, Tournament mergedTournament) {
        if (getTournamentCourseExecution().getCourseExecutionVersion() >= committedTournament.getTournamentCourseExecution().getCourseExecutionVersion()) {
            mergedTournament.getTournamentCourseExecution().setCourseExecutionVersion(getTournamentCourseExecution().getCourseExecutionVersion());
        } else {
            mergedTournament.getTournamentCourseExecution().setCourseExecutionVersion(committedTournament.getTournamentCourseExecution().getCourseExecutionVersion());
        }
    }

    private void mergeQuiz(Tournament committedTournament, Tournament mergedTournament) {
        // The quiz aggregate id must be set in case the quiz has been regenerated due to the previous having been invalidated
        if (getTournamentQuiz().getQuizVersion() >= committedTournament.getTournamentQuiz().getQuizVersion()) {
            mergedTournament.getTournamentQuiz().setQuizAggregateId(getTournamentQuiz().getQuizAggregateId());
            mergedTournament.getTournamentQuiz().setQuizVersion(getTournamentQuiz().getQuizVersion());
        } else {
            mergedTournament.getTournamentQuiz().setQuizVersion(committedTournament.getTournamentQuiz().getQuizAggregateId());
            mergedTournament.getTournamentQuiz().setQuizVersion(committedTournament.getTournamentQuiz().getQuizVersion());
        }

    }

    private void mergeCancelled(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("cancelled")) {
            mergedTournament.setCancelled(isCancelled());
        } else {
            mergedTournament.setCancelled(committedTournament.isCancelled());
        }
    }

    private void mergeNumberOfQuestions(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("numberOfQuestions")) {
            mergedTournament.setNumberOfQuestions(getNumberOfQuestions());
        } else {
            mergedTournament.setNumberOfQuestions(committedTournament.getNumberOfQuestions());
        }
    }

    private void mergeEndTime(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("endTime")) {
            mergedTournament.setEndTime(getEndTime());
        } else {
            mergedTournament.setEndTime(committedTournament.getEndTime());
        }
    }

    private void mergeStartTime(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("startTime")) {
            mergedTournament.setStartTime(getStartTime());
        } else {
            mergedTournament.setStartTime(committedTournament.getStartTime());
        }
    }

    private void mergeParticipants(Tournament prev, Tournament v1, Tournament v2, Tournament mergedTournament) {
        // Here we "calculate" the result of the incremental fields. This fields will always be the same regardless
        // of the base we choose.

        Set<TournamentParticipant> prevParticipantsPre = new HashSet<>(prev.getTournamentParticipants());
        Set<TournamentParticipant> v1ParticipantsPre = new HashSet<>(v1.getTournamentParticipants());
        Set<TournamentParticipant> v2ParticipantsPre = new HashSet<>(v2.getTournamentParticipants());

        Tournament.syncParticipantsVersions(prevParticipantsPre, v1ParticipantsPre, v2ParticipantsPre, prev.getTournamentCourseExecution().getCourseExecutionVersion(), v1.getTournamentCourseExecution().getCourseExecutionVersion(), v2.getTournamentCourseExecution().getCourseExecutionAggregateId());

        Set<TournamentParticipant> prevParticipants = new HashSet<>(prevParticipantsPre);
        Set<TournamentParticipant> v1Participants = new HashSet<>(v1ParticipantsPre);
        Set<TournamentParticipant> v2Participants = new HashSet<>(v2ParticipantsPre);


        Set<TournamentParticipant> addedParticipants =  SetUtils.union(
                SetUtils.difference(v1Participants, prevParticipants),
                SetUtils.difference(v2Participants, prevParticipants)
        );

        Set<TournamentParticipant> removedParticipants = SetUtils.union(
                SetUtils.difference(prevParticipants, v1Participants),
                SetUtils.difference(prevParticipants, v2Participants)
        );

        Set<TournamentParticipant> mergedParticipants = SetUtils.union(SetUtils.difference(prevParticipants, removedParticipants), addedParticipants);
        mergedTournament.setTournamentParticipants(mergedParticipants.stream().map(TournamentParticipant::new).collect(Collectors.toSet()));

    }

    private static void syncParticipantsVersions(Set<TournamentParticipant> prevParticipants,
                                                Set<TournamentParticipant> v1Participants,
                                                Set<TournamentParticipant> v2Participants,
                                                Integer prevCourseExecutionVersion,
                                                Integer v1CourseExecutionVersion,
                                                Integer v2CourseExecutionVersion) {

        for (TournamentParticipant tp1 : v1Participants) {
            for (TournamentParticipant tp2 : v2Participants) {
                if (tp1.getParticipantAggregateId().equals(tp2.getParticipantAggregateId())) {
                    if (v1CourseExecutionVersion > v2CourseExecutionVersion) {
                        tp2.setParticipantVersion(tp1.getParticipantVersion());
                        tp2.setParticipantName(tp1.getParticipantName());
                        tp2.setParticipantUsername(tp1.getParticipantUsername());
                        if (tp1.getParticipantAnswer() != null) {
                            tp2.setParticipantAnswer(new TournamentParticipantQuizAnswer(tp1.getParticipantAnswer()));
                        }
                    }

                    if (v2CourseExecutionVersion > v1CourseExecutionVersion) {
                        tp1.setParticipantVersion(tp2.getParticipantVersion());
                        tp1.setParticipantName(tp2.getParticipantName());
                        tp1.setParticipantUsername(tp2.getParticipantUsername());
                        if (tp2.getParticipantAnswer() != null) {
                            tp1.setParticipantAnswer(new TournamentParticipantQuizAnswer(tp2.getParticipantAnswer()));
                        }
                    }
                }
            }

            // no need to check again because the prev does not contain any newer version than v1 an v2
            for (TournamentParticipant prevParticipant : prevParticipants) {
                if (tp1.getParticipantAggregateId().equals(prevParticipant.getParticipantAggregateId())) {
                    if (v1CourseExecutionVersion > prevCourseExecutionVersion) {
                        prevParticipant.setParticipantVersion(tp1.getParticipantVersion());
                        prevParticipant.setParticipantName(tp1.getParticipantName());
                        prevParticipant.setParticipantUsername(tp1.getParticipantUsername());
                        if (tp1.getParticipantAnswer() != null) {
                            prevParticipant.setParticipantAnswer(new TournamentParticipantQuizAnswer(tp1.getParticipantAnswer()));
                        }
                    }

                    if (prevCourseExecutionVersion > v1CourseExecutionVersion) {
                        tp1.setParticipantVersion(prevParticipant.getParticipantVersion());
                        tp1.setParticipantName(prevParticipant.getParticipantName());
                        tp1.setParticipantUsername(prevParticipant.getParticipantUsername());
                        if (prevParticipant.getParticipantAnswer() != null) {
                            tp1.setParticipantAnswer(new TournamentParticipantQuizAnswer(prevParticipant.getParticipantAnswer()));
                        }
                    }
                }
            }
        }
    }

    private void mergeTopics(Set<String> toCommitVersionChangedFields, Tournament committedTournament, Tournament mergedTournament) {
        if (toCommitVersionChangedFields.contains("tournamentTopics")) {
            mergedTournament.setTournamentTopics(getTournamentTopics().stream().map(TournamentTopic::new).collect(Collectors.toSet()));
        } else {
            mergedTournament.setTournamentTopics(committedTournament.getTournamentTopics().stream().map(TournamentTopic::new).collect(Collectors.toSet()));
        }
    }

}
