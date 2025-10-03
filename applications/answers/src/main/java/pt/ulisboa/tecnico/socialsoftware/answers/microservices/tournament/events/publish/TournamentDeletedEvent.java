package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;


public class TournamentDeletedEvent extends ApplicationEvent implements Serializable {
private static final long serialVersionUID = 1L;

private final String eventId;
private final String eventType;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private final LocalDateTime timestamp;

private final Long aggregateId;

private final LocalDateTime startTime;
private final LocalDateTime endTime;
private final Integer numberOfQuestions;
private final Boolean cancelled;
private final TournamentCreator tournamentCreator;
private final Set<TournamentParticipant> tournamentParticipants;
private final TournamentExecution tournamentExecution;
private final Set<TournamentTopic> tournamentTopics;
private final TournamentQuiz tournamentQuiz;

public TournamentDeletedEvent(Object source, Long aggregateId, LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions, Boolean cancelled, TournamentCreator tournamentCreator, Set<TournamentParticipant> tournamentParticipants, TournamentExecution tournamentExecution, Set<TournamentTopic> tournamentTopics, TournamentQuiz tournamentQuiz) {
super(source);
this.eventId = java.util.UUID.randomUUID().toString();
this.eventType = "Deleted";
this.timestamp = LocalDateTime.now();
this.aggregateId = aggregateId;
this.startTime = startTime;
this.endTime = endTime;
this.numberOfQuestions = numberOfQuestions;
this.cancelled = cancelled;
this.tournamentCreator = tournamentCreator;
this.tournamentParticipants = tournamentParticipants;
this.tournamentExecution = tournamentExecution;
this.tournamentTopics = tournamentTopics;
this.tournamentQuiz = tournamentQuiz;
}

// Getters
public String getEventId() {
return eventId;
}

public String getEventType() {
return eventType;
}

public LocalDateTime getTimestamp() {
return timestamp;
}

public Long getAggregateId() {
return aggregateId;
}

public LocalDateTime getStartTime() {
return startTime;
}

public LocalDateTime getEndTime() {
return endTime;
}

public Integer getNumberOfQuestions() {
return numberOfQuestions;
}

public Boolean getCancelled() {
return cancelled;
}

public TournamentCreator getTournamentCreator() {
return tournamentCreator;
}

public Set<TournamentParticipant> getTournamentParticipants() {
return tournamentParticipants;
}

public TournamentExecution getTournamentExecution() {
return tournamentExecution;
}

public Set<TournamentTopic> getTournamentTopics() {
return tournamentTopics;
}

public TournamentQuiz getTournamentQuiz() {
return tournamentQuiz;
}

@Override
public String toString() {
return "TournamentDeletedEvent{" +
"eventId='" + eventId + '\'' +
", eventType='" + eventType + '\'' +
", timestamp=" + timestamp +
", aggregateId=" + aggregateId +
", startTime=" + startTime +
", endTime=" + endTime +
", numberOfQuestions=" + numberOfQuestions +
", cancelled=" + cancelled +
", tournamentCreator=" + tournamentCreator +
", tournamentParticipants=" + tournamentParticipants +
", tournamentExecution=" + tournamentExecution +
", tournamentTopics=" + tournamentTopics +
", tournamentQuiz=" + tournamentQuiz +
'}';
}
}