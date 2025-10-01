package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;


@Component
public class DeletedSubscription {
private static final Logger logger = LoggerFactory.getLogger(DeletedSubscription.class);

private final UserService userService;

public DeletedSubscription(UserService userService) {
this.userService = userService;
}

@EventListener
@Async
public void handleDeleted(UserDeletedEvent event) {
try {
logger.info("Handling Deleted event for User with ID: {}", event.getAggregateId());

// Process the Deleted event
processDeleted(event);

} catch (Exception e) {
logger.error("Error handling UserDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
// Consider implementing retry logic or dead letter queue
}
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAfterCommitDeleted(UserDeletedEvent event) {
try {
logger.info("Post-commit handling Deleted event for User with ID: {}",
event.getAggregateId());

// Post-transaction processing
postProcessDeleted(event);

} catch (Exception e) {
logger.error("Error in post-commit handling of UserDeletedEvent for aggregate ID: {}",
event.getAggregateId(), e);
}
}

private void processDeleted(UserDeletedEvent event) {
// TODO: Implement Deleted event processing logic
switch (event.getEventType()) {
case "Deleted":
handleDeletedLogic(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}
}

private void postProcessDeleted(UserDeletedEvent event) {
// TODO: Implement post-transaction Deleted event processing
// This could include:
// - Sending notifications
// - Updating external systems
// - Triggering workflows
logger.debug("Post-processing Deleted event completed for aggregate ID: {}", event.getAggregateId());
}

private void handleDeletedLogic(UserDeletedEvent event) {
// TODO: Implement specific Deleted business logic
logger.debug("Processing Deleted logic for aggregate ID: {}", event.getAggregateId());
}
}