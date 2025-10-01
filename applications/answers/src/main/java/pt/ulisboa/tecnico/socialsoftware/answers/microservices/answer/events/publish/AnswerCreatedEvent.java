package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonFormat;


public class AnswerCreatedEvent extends ApplicationEvent implements Serializable {
private static final long serialVersionUID = 1L;

private final String eventId;
private final String eventType;

@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
private final LocalDateTime timestamp;

private final Long aggregateId;

private final LocalDateTime answerDate;
private final LocalDateTime completedDate;
private final Boolean completed;
private final Object quizAnswerStudent;
private final Object quizAnswerCourseExecution;
private final Object questionAnswers;
private final Object answeredQuiz;

public AnswerCreatedEvent(Object source, Long aggregateId, LocalDateTime answerDate, LocalDateTime completedDate, Boolean completed, Object quizAnswerStudent, Object quizAnswerCourseExecution, Object questionAnswers, Object answeredQuiz) {
super(source);
this.eventId = java.util.UUID.randomUUID().toString();
this.eventType = "Created";
this.timestamp = LocalDateTime.now();
this.aggregateId = aggregateId;
this.answerDate = answerDate;
this.completedDate = completedDate;
this.completed = completed;
this.quizAnswerStudent = quizAnswerStudent;
this.quizAnswerCourseExecution = quizAnswerCourseExecution;
this.questionAnswers = questionAnswers;
this.answeredQuiz = answeredQuiz;
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

public LocalDateTime getAnswerDate() {
return answerDate;
}

public LocalDateTime getCompletedDate() {
return completedDate;
}

public Boolean getCompleted() {
return completed;
}

public Object getQuizAnswerStudent() {
return quizAnswerStudent;
}

public Object getQuizAnswerCourseExecution() {
return quizAnswerCourseExecution;
}

public Object getQuestionAnswers() {
return questionAnswers;
}

public Object getAnsweredQuiz() {
return answeredQuiz;
}

@Override
public String toString() {
return "AnswerCreatedEvent{" +
"eventId='" + eventId + '\'' +
", eventType='" + eventType + '\'' +
", timestamp=" + timestamp +
", aggregateId=" + aggregateId +
", answerDate=" + answerDate +
", completedDate=" + completedDate +
", completed=" + completed +
", quizAnswerStudent=" + quizAnswerStudent +
", quizAnswerCourseExecution=" + quizAnswerCourseExecution +
", questionAnswers=" + questionAnswers +
", answeredQuiz=" + answeredQuiz +
'}';
}
}