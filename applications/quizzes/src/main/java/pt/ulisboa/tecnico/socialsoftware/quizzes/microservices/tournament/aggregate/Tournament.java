package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.CANNOT_ADD_PARTICIPANT;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.CANNOT_DELETE_TOURNAMENT;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.CANNOT_UPDATE_TOURNAMENT;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesAnonymizeStudent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesAnswerQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesDeleteCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesDeleteTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesDisenrollStudentFromCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesInvalidateQuiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesUpdateStudentName;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe.TournamentSubscribesUpdateTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

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
public abstract class Tournament extends Aggregate {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private boolean cancelled;

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentCreator tournamentCreator;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "tournament")
    private Set<TournamentParticipant> tournamentParticipants = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentCourseExecution tournamentCourseExecution;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "tournament")
    private Set<TournamentTopic> tournamentTopics = new HashSet<>();

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
            throw new QuizzesException(CANNOT_DELETE_TOURNAMENT, getAggregateId());
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
        return this.tournamentParticipants.stream()
                .noneMatch(p -> p.getParticipantAggregateId().equals(this.tournamentCreator.getCreatorAggregateId())
                        && (!p.getParticipantVersion().equals(this.tournamentCreator.getCreatorVersion())
                        || !p.getParticipantName().equals(this.tournamentCreator.getCreatorName())
                        || !p.getParticipantUsername().equals(this.tournamentCreator.getCreatorUsername())));
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
            throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
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
                throw new QuizzesException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
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
                throw new QuizzesException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
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
                throw new QuizzesException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
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
                throw new QuizzesException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
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
            throw new QuizzesException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
        }*/
        this.tournamentParticipants = tournamentParticipants;
        this.tournamentParticipants.forEach(tournamentParticipant -> tournamentParticipant.setTournament(this));
    }

    public void addParticipant(TournamentParticipant participant) {
        // INV: tournamentParticipants is final after start time
        Tournament prev = (Tournament) getPrev();
        if (DateHandler.now().isAfter(prev.getStartTime())) {
            throw new QuizzesException(CANNOT_ADD_PARTICIPANT, getAggregateId());
        }
        /*
        IS_CANCELED
		    this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
         */
        if (prev != null && prev.isCancelled()) {
            throw new QuizzesException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
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
                throw new QuizzesException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
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
                throw new QuizzesException(CANNOT_UPDATE_TOURNAMENT, getAggregateId());
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
}
