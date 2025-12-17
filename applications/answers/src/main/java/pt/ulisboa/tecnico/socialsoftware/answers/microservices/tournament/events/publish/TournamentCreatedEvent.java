package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class TournamentCreatedEvent extends Event {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer numberOfQuestions;
    private Boolean cancelled;
    private TournamentCreator creator;
    private Set<TournamentParticipant> participants;
    private TournamentExecution execution;
    private Set<TournamentTopic> topics;
    private TournamentQuiz quiz;

    public TournamentCreatedEvent() {
    }

    public TournamentCreatedEvent(Integer aggregateId, LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions, Boolean cancelled, TournamentCreator creator, Set<TournamentParticipant> participants, TournamentExecution execution, Set<TournamentTopic> topics, TournamentQuiz quiz) {
        super(aggregateId);
        setStartTime(startTime);
        setEndTime(endTime);
        setNumberOfQuestions(numberOfQuestions);
        setCancelled(cancelled);
        setCreator(creator);
        setParticipants(participants);
        setExecution(execution);
        setTopics(topics);
        setQuiz(quiz);
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
    }

    public Set<TournamentParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<TournamentParticipant> participants) {
        this.participants = participants;
    }

    public TournamentExecution getExecution() {
        return execution;
    }

    public void setExecution(TournamentExecution execution) {
        this.execution = execution;
    }

    public Set<TournamentTopic> getTopics() {
        return topics;
    }

    public void setTopics(Set<TournamentTopic> topics) {
        this.topics = topics;
    }

    public TournamentQuiz getQuiz() {
        return quiz;
    }

    public void setQuiz(TournamentQuiz quiz) {
        this.quiz = quiz;
    }

}