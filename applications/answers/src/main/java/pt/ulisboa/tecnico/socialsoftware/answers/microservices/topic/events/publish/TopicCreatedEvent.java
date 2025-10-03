package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;


public class TopicCreatedEvent extends ApplicationEvent implements Serializable {
private static final long serialVersionUID = 1L;

private final String eventId;
private final String eventType;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private final LocalDateTime timestamp;

private final Long aggregateId;

private final String name;
private final TopicCourse course;
private final LocalDateTime creationDate;

public TopicCreatedEvent(Object source, Long aggregateId, String name, TopicCourse course, LocalDateTime creationDate) {
super(source);
this.eventId = java.util.UUID.randomUUID().toString();
this.eventType = "Created";
this.timestamp = LocalDateTime.now();
this.aggregateId = aggregateId;
this.name = name;
this.course = course;
this.creationDate = creationDate;
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

public TopicCourse getCourse() {
return course;
}

public LocalDateTime getCreationDate() {
return creationDate;
}

@Override
public String toString() {
return "TopicCreatedEvent{" +
"eventId='" + eventId + '\'' +
", eventType='" + eventType + '\'' +
", timestamp=" + timestamp +
", aggregateId=" + aggregateId +
", name=" + name +
", course=" + course +
", creationDate=" + creationDate +
'}';
}
}