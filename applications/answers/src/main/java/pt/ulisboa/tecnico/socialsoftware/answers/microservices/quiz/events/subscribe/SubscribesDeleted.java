package com.generated.microservices.answers.microservices.quiz.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.quiz.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;


@Component
public class DeletedSubscription {
private static final Logger logger = LoggerFactory.getLogger(DeletedSubscription.class);

private final QuizService quizService;

public DeletedSubscription(QuizService quizService) {
this.quizService = quizService;
}

@EventListener
@Async
public void handleDeleted(QuizDeletedEvent event) {
try {
logger.info("Handling Deleted event for Quiz with ID: {}", event.getAggregateId());

// Process the Deleted event
processDeleted(event);

} catch (Exception e) {
logger.error("Error handling QuizDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
// Consider implementing retry logic or dead letter queue
}
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAfterCommitDeleted(QuizDeletedEvent event) {
try {
logger.info("Post-commit handling Deleted event for Quiz with ID: {}",
event.getAggregateId());

// Post-transaction processing
postProcessDeleted(event);

} catch (Exception e) {
logger.error("Error in post-commit handling of QuizDeletedEvent for aggregate ID: {}",
event.getAggregateId(), e);
}
}

private void processDeleted(QuizDeletedEvent event) {
// TODO: Implement Deleted event processing logic
switch (event.getEventType()) {
case "Deleted":
handleDeletedLogic(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}
}

private void postProcessDeleted(QuizDeletedEvent event) {
// TODO: Implement post-transaction Deleted event processing
// This could include:
// - Sending notifications
// - Updating external systems
// - Triggering workflows
logger.debug("Post-processing Deleted event completed for aggregate ID: {}", event.getAggregateId());
}

private void handleDeletedLogic(QuizDeletedEvent event) {
// TODO: Implement specific Deleted business logic
logger.debug("Processing Deleted logic for aggregate ID: {}", event.getAggregateId());
}
}