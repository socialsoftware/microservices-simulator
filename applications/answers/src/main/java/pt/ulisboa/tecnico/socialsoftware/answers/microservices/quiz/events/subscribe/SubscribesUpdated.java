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
public class UpdatedSubscription {
private static final Logger logger = LoggerFactory.getLogger(UpdatedSubscription.class);

private final QuizService quizService;

public UpdatedSubscription(QuizService quizService) {
this.quizService = quizService;
}

@EventListener
@Async
public void handleUpdated(QuizUpdatedEvent event) {
try {
logger.info("Handling Updated event for Quiz with ID: {}", event.getAggregateId());

// Process the Updated event
processUpdated(event);

} catch (Exception e) {
logger.error("Error handling QuizUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
// Consider implementing retry logic or dead letter queue
}
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAfterCommitUpdated(QuizUpdatedEvent event) {
try {
logger.info("Post-commit handling Updated event for Quiz with ID: {}",
event.getAggregateId());

// Post-transaction processing
postProcessUpdated(event);

} catch (Exception e) {
logger.error("Error in post-commit handling of QuizUpdatedEvent for aggregate ID: {}",
event.getAggregateId(), e);
}
}

private void processUpdated(QuizUpdatedEvent event) {
// TODO: Implement Updated event processing logic
switch (event.getEventType()) {
case "Updated":
handleUpdatedLogic(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}
}

private void postProcessUpdated(QuizUpdatedEvent event) {
// TODO: Implement post-transaction Updated event processing
// This could include:
// - Sending notifications
// - Updating external systems
// - Triggering workflows
logger.debug("Post-processing Updated event completed for aggregate ID: {}", event.getAggregateId());
}

private void handleUpdatedLogic(QuizUpdatedEvent event) {
// TODO: Implement specific Updated business logic
logger.debug("Processing Updated logic for aggregate ID: {}", event.getAggregateId());
}
}