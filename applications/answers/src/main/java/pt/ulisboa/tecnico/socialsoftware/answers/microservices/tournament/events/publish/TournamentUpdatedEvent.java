package com.generated.microservices.answers.microservices.tournament.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.tournament.aggregate.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;


public class TournamentUpdatedEvent extends ApplicationEvent implements Serializable {
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
private final Object tournamentCreator;
private final Object tournamentParticipants;
private final Object tournamentCourseExecution;
private final Object tournamentTopics;
private final Object tournamentQuiz;

public TournamentUpdatedEvent(Object source, Long aggregateId, LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions, Boolean cancelled, Object tournamentCreator, Object tournamentParticipants, Object tournamentCourseExecution, Object tournamentTopics, Object tournamentQuiz) {
super(source);
this.eventId = java.util.UUID.randomUUID().toString();
this.eventType = "Updated";
this.timestamp = LocalDateTime.now();
this.aggregateId = aggregateId;
this.startTime = startTime;
this.endTime = endTime;
this.numberOfQuestions = numberOfQuestions;
this.cancelled = cancelled;
this.tournamentCreator = tournamentCreator;
this.tournamentParticipants = tournamentParticipants;
this.tournamentCourseExecution = tournamentCourseExecution;
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

public Object getTournamentCreator() {
return tournamentCreator;
}

public Object getTournamentParticipants() {
return tournamentParticipants;
}

public Object getTournamentCourseExecution() {
return tournamentCourseExecution;
}

public Object getTournamentTopics() {
return tournamentTopics;
}

public Object getTournamentQuiz() {
return tournamentQuiz;
}

@Override
public String toString() {
return "TournamentUpdatedEvent{" +
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
", tournamentCourseExecution=" + tournamentCourseExecution +
", tournamentTopics=" + tournamentTopics +
", tournamentQuiz=" + tournamentQuiz +
'}';
}
}