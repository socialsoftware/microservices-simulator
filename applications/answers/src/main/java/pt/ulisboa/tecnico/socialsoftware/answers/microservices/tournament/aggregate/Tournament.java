package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe.TournamentSubscribesExecutionDeletedExecutionRef;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe.TournamentSubscribesExecutionUpdated;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe.TournamentSubscribesExecutionUserUpdated;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe.TournamentSubscribesQuizDeletedQuizRef;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe.TournamentSubscribesQuizUpdated;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe.TournamentSubscribesTopicDeletedTopicsRef;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe.TournamentSubscribesTopicUpdated;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe.TournamentSubscribesUserDeletedCreatorRef;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe.TournamentSubscribesUserDeletedParticipantsRef;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentCreatorDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentQuizDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Tournament extends Aggregate {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentCreator creator;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "tournament")
    private Set<TournamentParticipant> participants = new HashSet<>();
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentExecution execution;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "tournament")
    private Set<TournamentTopic> topics = new HashSet<>();
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournament")
    private TournamentQuiz quiz;

    public Tournament() {

    }

    public Tournament(Integer aggregateId, TournamentDto tournamentDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setStartTime(tournamentDto.getStartTime());
        setEndTime(tournamentDto.getEndTime());
        setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
        setCancelled(tournamentDto.getCancelled());
        setCreator(tournamentDto.getCreator() != null ? new TournamentCreator(tournamentDto.getCreator()) : null);
        setExecution(tournamentDto.getExecution() != null ? new TournamentExecution(tournamentDto.getExecution()) : null);
        setQuiz(tournamentDto.getQuiz() != null ? new TournamentQuiz(tournamentDto.getQuiz()) : null);
        setParticipants(tournamentDto.getParticipants() != null ? tournamentDto.getParticipants().stream().map(TournamentParticipant::new).collect(Collectors.toSet()) : null);
        setTopics(tournamentDto.getTopics() != null ? tournamentDto.getTopics().stream().map(TournamentTopic::new).collect(Collectors.toSet()) : null);
    }


    public Tournament(Tournament other) {
        super(other);
        setStartTime(other.getStartTime());
        setEndTime(other.getEndTime());
        setNumberOfQuestions(other.getNumberOfQuestions());
        setCancelled(other.getCancelled());
        setCreator(other.getCreator() != null ? new TournamentCreator(other.getCreator()) : null);
        setParticipants(other.getParticipants() != null ? other.getParticipants().stream().map(TournamentParticipant::new).collect(Collectors.toSet()) : null);
        setExecution(other.getExecution() != null ? new TournamentExecution(other.getExecution()) : null);
        setTopics(other.getTopics() != null ? other.getTopics().stream().map(TournamentTopic::new).collect(Collectors.toSet()) : null);
        setQuiz(other.getQuiz() != null ? new TournamentQuiz(other.getQuiz()) : null);
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

    public TournamentCreator getCreator() {
        return creator;
    }

    public void setCreator(TournamentCreator creator) {
        this.creator = creator;
        if (this.creator != null) {
            this.creator.setTournament(this);
        }
    }

    public Set<TournamentParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<TournamentParticipant> participants) {
        this.participants = participants;
        if (this.participants != null) {
            this.participants.forEach(item -> item.setTournament(this));
        }
    }

    public void addTournamentParticipant(TournamentParticipant tournamentParticipant) {
        if (this.participants == null) {
            this.participants = new HashSet<>();
        }
        this.participants.add(tournamentParticipant);
        if (tournamentParticipant != null) {
            tournamentParticipant.setTournament(this);
        }
    }

    public void removeTournamentParticipant(Integer id) {
        if (this.participants != null) {
            this.participants.removeIf(item -> 
                item.getParticipantAggregateId() != null && item.getParticipantAggregateId().equals(id));
        }
    }

    public boolean containsTournamentParticipant(Integer id) {
        if (this.participants == null) {
            return false;
        }
        return this.participants.stream().anyMatch(item -> 
            item.getParticipantAggregateId() != null && item.getParticipantAggregateId().equals(id));
    }

    public TournamentParticipant findTournamentParticipantById(Integer id) {
        if (this.participants == null) {
            return null;
        }
        return this.participants.stream()
            .filter(item -> item.getParticipantAggregateId() != null && item.getParticipantAggregateId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public TournamentExecution getExecution() {
        return execution;
    }

    public void setExecution(TournamentExecution execution) {
        this.execution = execution;
        if (this.execution != null) {
            this.execution.setTournament(this);
        }
    }

    public Set<TournamentTopic> getTopics() {
        return topics;
    }

    public void setTopics(Set<TournamentTopic> topics) {
        this.topics = topics;
        if (this.topics != null) {
            this.topics.forEach(item -> item.setTournament(this));
        }
    }

    public void addTournamentTopic(TournamentTopic tournamentTopic) {
        if (this.topics == null) {
            this.topics = new HashSet<>();
        }
        this.topics.add(tournamentTopic);
        if (tournamentTopic != null) {
            tournamentTopic.setTournament(this);
        }
    }

    public void removeTournamentTopic(Integer id) {
        if (this.topics != null) {
            this.topics.removeIf(item -> 
                item.getTopicAggregateId() != null && item.getTopicAggregateId().equals(id));
        }
    }

    public boolean containsTournamentTopic(Integer id) {
        if (this.topics == null) {
            return false;
        }
        return this.topics.stream().anyMatch(item -> 
            item.getTopicAggregateId() != null && item.getTopicAggregateId().equals(id));
    }

    public TournamentTopic findTournamentTopicById(Integer id) {
        if (this.topics == null) {
            return null;
        }
        return this.topics.stream()
            .filter(item -> item.getTopicAggregateId() != null && item.getTopicAggregateId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public TournamentQuiz getQuiz() {
        return quiz;
    }

    public void setQuiz(TournamentQuiz quiz) {
        this.quiz = quiz;
        if (this.quiz != null) {
            this.quiz.setTournament(this);
        }
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantExecutionRef(eventSubscriptions);
            interInvariantCreatorRef(eventSubscriptions);
            interInvariantParticipantsRef(eventSubscriptions);
            interInvariantTopicsRef(eventSubscriptions);
            interInvariantQuizRef(eventSubscriptions);
            eventSubscriptions.add(new TournamentSubscribesExecutionUpdated());
            eventSubscriptions.add(new TournamentSubscribesExecutionUserUpdated());
            eventSubscriptions.add(new TournamentSubscribesTopicUpdated());
            eventSubscriptions.add(new TournamentSubscribesQuizUpdated());
        }
        return eventSubscriptions;
    }
    private void interInvariantExecutionRef(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TournamentSubscribesExecutionDeletedExecutionRef(this.getExecution()));
    }

    private void interInvariantCreatorRef(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TournamentSubscribesUserDeletedCreatorRef(this.getCreator()));
    }

    private void interInvariantParticipantsRef(Set<EventSubscription> eventSubscriptions) {
        for (TournamentParticipant item : this.participants) {
            eventSubscriptions.add(new TournamentSubscribesUserDeletedParticipantsRef(item));
        }
    }

    private void interInvariantTopicsRef(Set<EventSubscription> eventSubscriptions) {
        for (TournamentTopic item : this.topics) {
            eventSubscriptions.add(new TournamentSubscribesTopicDeletedTopicsRef(item));
        }
    }

    private void interInvariantQuizRef(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new TournamentSubscribesQuizDeletedQuizRef(this.getQuiz()));
    }


    private boolean invariantRule0() {
        return this.startTime.isBefore(this.endTime);
    }

    private boolean invariantRule1() {
        return this.participants != null;
    }

    private boolean invariantRule2() {
        return this.creator != null;
    }

    private boolean invariantRule3() {
        return this.execution != null;
    }

    private boolean invariantRule4() {
        return this.topics != null;
    }

    private boolean invariantRule5() {
        return this.quiz != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantRule0()) {
            throw new SimulatorException(INVARIANT_BREAK, "Tournament start time must be before end time");
        }
        if (!invariantRule1()) {
            throw new SimulatorException(INVARIANT_BREAK, "Tournament must have a participants collection");
        }
        if (!invariantRule2()) {
            throw new SimulatorException(INVARIANT_BREAK, "Tournament must have a creator");
        }
        if (!invariantRule3()) {
            throw new SimulatorException(INVARIANT_BREAK, "Tournament must be associated with an execution");
        }
        if (!invariantRule4()) {
            throw new SimulatorException(INVARIANT_BREAK, "Tournament must have a topics collection");
        }
        if (!invariantRule5()) {
            throw new SimulatorException(INVARIANT_BREAK, "Tournament must have a quiz");
        }
    }

    public TournamentDto buildDto() {
        TournamentDto dto = new TournamentDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setStartTime(getStartTime());
        dto.setEndTime(getEndTime());
        dto.setNumberOfQuestions(getNumberOfQuestions());
        dto.setCancelled(getCancelled());
        dto.setCreator(getCreator() != null ? new TournamentCreatorDto(getCreator()) : null);
        dto.setParticipants(getParticipants() != null ? getParticipants().stream().map(TournamentParticipant::buildDto).collect(Collectors.toSet()) : null);
        dto.setExecution(getExecution() != null ? new TournamentExecutionDto(getExecution()) : null);
        dto.setTopics(getTopics() != null ? getTopics().stream().map(TournamentTopic::buildDto).collect(Collectors.toSet()) : null);
        dto.setQuiz(getQuiz() != null ? new TournamentQuizDto(getQuiz()) : null);
        return dto;
    }
}