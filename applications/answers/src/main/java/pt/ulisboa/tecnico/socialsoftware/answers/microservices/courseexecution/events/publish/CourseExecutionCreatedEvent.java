package com.generated.microservices.answers.microservices.courseexecution.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.courseexecution.aggregate.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;


public class CourseExecutionCreatedEvent extends ApplicationEvent implements Serializable {
private static final long serialVersionUID = 1L;

private final String eventId;
private final String eventType;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private final LocalDateTime timestamp;

private final Long aggregateId;

private final String name;
private final String acronym;
private final String academicTerm;
private final LocalDateTime startDate;
private final LocalDateTime endDate;
private final Object course;
private final Object students;

public CourseExecutionCreatedEvent(Object source, Long aggregateId, String name, String acronym, String academicTerm, LocalDateTime startDate, LocalDateTime endDate, Object course, Object students) {
super(source);
this.eventId = java.util.UUID.randomUUID().toString();
this.eventType = "Created";
this.timestamp = LocalDateTime.now();
this.aggregateId = aggregateId;
this.name = name;
this.acronym = acronym;
this.academicTerm = academicTerm;
this.startDate = startDate;
this.endDate = endDate;
this.course = course;
this.students = students;
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

public String getAcronym() {
return acronym;
}

public String getAcademicTerm() {
return academicTerm;
}

public LocalDateTime getStartDate() {
return startDate;
}

public LocalDateTime getEndDate() {
return endDate;
}

public Object getCourse() {
return course;
}

public Object getStudents() {
return students;
}

@Override
public String toString() {
return "CourseExecutionCreatedEvent{" +
"eventId='" + eventId + '\'' +
", eventType='" + eventType + '\'' +
", timestamp=" + timestamp +
", aggregateId=" + aggregateId +
", name=" + name +
", acronym=" + acronym +
", academicTerm=" + academicTerm +
", startDate=" + startDate +
", endDate=" + endDate +
", course=" + course +
", students=" + students +
'}';
}
}