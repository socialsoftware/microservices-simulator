package com.generated.microservices.answers.microservices.user.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.user.aggregate.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;


public class UserDeletedEvent extends ApplicationEvent implements Serializable {
private static final long serialVersionUID = 1L;

private final String eventId;
private final String eventType;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private final LocalDateTime timestamp;

private final Long aggregateId;

private final String name;
private final String username;
private final Boolean active;

public UserDeletedEvent(Object source, Long aggregateId, String name, String username, Boolean active) {
super(source);
this.eventId = java.util.UUID.randomUUID().toString();
this.eventType = "Deleted";
this.timestamp = LocalDateTime.now();
this.aggregateId = aggregateId;
this.name = name;
this.username = username;
this.active = active;
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

public String getName() {
return name;
}

public String getUsername() {
return username;
}

public Boolean getActive() {
return active;
}

@Override
public String toString() {
return "UserDeletedEvent{" +
"eventId='" + eventId + '\'' +
", eventType='" + eventType + '\'' +
", timestamp=" + timestamp +
", aggregateId=" + aggregateId +
", name=" + name +
", username=" + username +
", active=" + active +
'}';
}
}