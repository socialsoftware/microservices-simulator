package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;


@Component
public class UpdatedSubscription {
private static final Logger logger = LoggerFactory.getLogger(UpdatedSubscription.class);

private final QuestionService questionService;

public UpdatedSubscription(QuestionService questionService) {
this.questionService = questionService;
}

@EventListener
@Async
public void handleUpdated(QuestionUpdatedEvent event) {
try {
logger.info("Handling Updated event for Question with ID: {}", event.getAggregateId());

// Process the Updated event
processUpdated(event);

} catch (Exception e) {
logger.error("Error handling QuestionUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
// Consider implementing retry logic or dead letter queue
}
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAfterCommitUpdated(QuestionUpdatedEvent event) {
try {
logger.info("Post-commit handling Updated event for Question with ID: {}",
event.getAggregateId());

// Post-transaction processing
postProcessUpdated(event);

} catch (Exception e) {
logger.error("Error in post-commit handling of QuestionUpdatedEvent for aggregate ID: {}",
event.getAggregateId(), e);
}
}

private void processUpdated(QuestionUpdatedEvent event) {
// TODO: Implement Updated event processing logic
switch (event.getEventType()) {
case "Updated":
handleUpdatedLogic(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}
}

private void postProcessUpdated(QuestionUpdatedEvent event) {
// TODO: Implement post-transaction Updated event processing
// This could include:
// - Sending notifications
// - Updating external systems
// - Triggering workflows
logger.debug("Post-processing Updated event completed for aggregate ID: {}", event.getAggregateId());
}

private void handleUpdatedLogic(QuestionUpdatedEvent event) {
// TODO: Implement specific Updated business logic
logger.debug("Processing Updated logic for aggregate ID: {}", event.getAggregateId());
}
}