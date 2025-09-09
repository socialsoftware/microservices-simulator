package com.generated.microservices.answers.microservices.question.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.question.aggregate.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;


public class QuestionCreatedEvent extends ApplicationEvent implements Serializable {
private static final long serialVersionUID = 1L;

private final String eventId;
private final String eventType;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private final LocalDateTime timestamp;

private final Long aggregateId;

private final String title;
private final String content;
private final Integer numberOfOptions;
private final Integer correctOption;
private final Integer order;
private final Object course;
private final Object topics;
private final Object options;

public QuestionCreatedEvent(Object source, Long aggregateId, String title, String content, Integer numberOfOptions, Integer correctOption, Integer order, Object course, Object topics, Object options) {
super(source);
this.eventId = java.util.UUID.randomUUID().toString();
this.eventType = "Created";
this.timestamp = LocalDateTime.now();
this.aggregateId = aggregateId;
this.title = title;
this.content = content;
this.numberOfOptions = numberOfOptions;
this.correctOption = correctOption;
this.order = order;
this.course = course;
this.topics = topics;
this.options = options;
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

public String getTitle() {
return title;
}

public String getContent() {
return content;
}

public Integer getNumberOfOptions() {
return numberOfOptions;
}

public Integer getCorrectOption() {
return correctOption;
}

public Integer getOrder() {
return order;
}

public Object getCourse() {
return course;
}

public Object getTopics() {
return topics;
}

public Object getOptions() {
return options;
}

@Override
public String toString() {
return "QuestionCreatedEvent{" +
"eventId='" + eventId + '\'' +
", eventType='" + eventType + '\'' +
", timestamp=" + timestamp +
", aggregateId=" + aggregateId +
", title=" + title +
", content=" + content +
", numberOfOptions=" + numberOfOptions +
", correctOption=" + correctOption +
", order=" + order +
", course=" + course +
", topics=" + topics +
", options=" + options +
'}';
}
}