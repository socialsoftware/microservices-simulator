package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;


public class QuizUpdatedEvent extends ApplicationEvent implements Serializable {
private static final long serialVersionUID = 1L;

private final String eventId;
private final String eventType;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private final LocalDateTime timestamp;

private final Long aggregateId;

private final String title;
private final String description;
private final String quizType;
private final LocalDateTime availableDate;
private final LocalDateTime conclusionDate;
private final Integer numberOfQuestions;
private final Object courseExecution;
private final Object questions;
private final Object options;

public QuizUpdatedEvent(Object source, Long aggregateId, String title, String description, String quizType, LocalDateTime availableDate, LocalDateTime conclusionDate, Integer numberOfQuestions, Object courseExecution, Object questions, Object options) {
super(source);
this.eventId = java.util.UUID.randomUUID().toString();
this.eventType = "Updated";
this.timestamp = LocalDateTime.now();
this.aggregateId = aggregateId;
this.title = title;
this.description = description;
this.quizType = quizType;
this.availableDate = availableDate;
this.conclusionDate = conclusionDate;
this.numberOfQuestions = numberOfQuestions;
this.courseExecution = courseExecution;
this.questions = questions;
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

public String getDescription() {
return description;
}

public String getQuizType() {
return quizType;
}

public LocalDateTime getAvailableDate() {
return availableDate;
}

public LocalDateTime getConclusionDate() {
return conclusionDate;
}

public Integer getNumberOfQuestions() {
return numberOfQuestions;
}

public Object getCourseExecution() {
return courseExecution;
}

public Object getQuestions() {
return questions;
}

public Object getOptions() {
return options;
}

@Override
public String toString() {
return "QuizUpdatedEvent{" +
"eventId='" + eventId + '\'' +
", eventType='" + eventType + '\'' +
", timestamp=" + timestamp +
", aggregateId=" + aggregateId +
", title=" + title +
", description=" + description +
", quizType=" + quizType +
", availableDate=" + availableDate +
", conclusionDate=" + conclusionDate +
", numberOfQuestions=" + numberOfQuestions +
", courseExecution=" + courseExecution +
", questions=" + questions +
", options=" + options +
'}';
}
}