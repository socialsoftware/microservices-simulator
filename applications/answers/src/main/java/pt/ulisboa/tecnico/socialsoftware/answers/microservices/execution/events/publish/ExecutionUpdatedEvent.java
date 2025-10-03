package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;


public class ExecutionUpdatedEvent extends ApplicationEvent implements Serializable {
private static final long serialVersionUID = 1L;

private final String eventId;
private final String eventType;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private final LocalDateTime timestamp;

private final Long aggregateId;

private final String acronym;
private final String academicTerm;
private final LocalDateTime endDate;
private final ExecutionCourse executionCourse;
private final Set<ExecutionStudent> students;

public ExecutionUpdatedEvent(Object source, Long aggregateId, String acronym, String academicTerm, LocalDateTime endDate, ExecutionCourse executionCourse, Set<ExecutionStudent> students) {
super(source);
this.eventId = java.util.UUID.randomUUID().toString();
this.eventType = "Updated";
this.timestamp = LocalDateTime.now();
this.aggregateId = aggregateId;
this.acronym = acronym;
this.academicTerm = academicTerm;
this.endDate = endDate;
this.executionCourse = executionCourse;
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

public String getAcronym() {
return acronym;
}

public String getAcademicTerm() {
return academicTerm;
}

public LocalDateTime getEndDate() {
return endDate;
}

public ExecutionCourse getExecutionCourse() {
return executionCourse;
}

public Set<ExecutionStudent> getStudents() {
return students;
}

@Override
public String toString() {
return "ExecutionUpdatedEvent{" +
"eventId='" + eventId + '\'' +
", eventType='" + eventType + '\'' +
", timestamp=" + timestamp +
", aggregateId=" + aggregateId +
", acronym=" + acronym +
", academicTerm=" + academicTerm +
", endDate=" + endDate +
", executionCourse=" + executionCourse +
", students=" + students +
'}';
}
}