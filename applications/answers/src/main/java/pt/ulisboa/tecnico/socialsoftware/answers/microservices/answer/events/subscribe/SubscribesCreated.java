package com.generated.microservices.answers.microservices.answer.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.answer.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;


@Component
public class CreatedSubscription {
private static final Logger logger = LoggerFactory.getLogger(CreatedSubscription.class);

private final AnswerService answerService;

public CreatedSubscription(AnswerService answerService) {
this.answerService = answerService;
}

@EventListener
@Async
public void handleCreated(AnswerCreatedEvent event) {
try {
logger.info("Handling Created event for Answer with ID: {}", event.getAggregateId());

// Process the Created event
processCreated(event);

} catch (Exception e) {
logger.error("Error handling AnswerCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
// Consider implementing retry logic or dead letter queue
}
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAfterCommitCreated(AnswerCreatedEvent event) {
try {
logger.info("Post-commit handling Created event for Answer with ID: {}",
event.getAggregateId());

// Post-transaction processing
postProcessCreated(event);

} catch (Exception e) {
logger.error("Error in post-commit handling of AnswerCreatedEvent for aggregate ID: {}",
event.getAggregateId(), e);
}
}

private void processCreated(AnswerCreatedEvent event) {
// TODO: Implement Created event processing logic
switch (event.getEventType()) {
case "Created":
handleCreatedLogic(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}
}

private void postProcessCreated(AnswerCreatedEvent event) {
// TODO: Implement post-transaction Created event processing
// This could include:
// - Sending notifications
// - Updating external systems
// - Triggering workflows
logger.debug("Post-processing Created event completed for aggregate ID: {}", event.getAggregateId());
}

private void handleCreatedLogic(AnswerCreatedEvent event) {
// TODO: Implement specific Created business logic
logger.debug("Processing Created logic for aggregate ID: {}", event.getAggregateId());
}
}