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

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;


@Component
public class UserEventHandling {
private static final Logger logger = LoggerFactory.getLogger(UserEventHandling.class);

private final UserService userService;
private final UserRepository userRepository;

public UserEventHandling(UserService userService, UserRepository
userRepository) {
this.userService = userService;
this.userRepository = userRepository;
}

@EventListener
public void handleCreated(UserCreatedEvent event) {
try {
logger.info("Processing Created event for User with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Created":
processCreatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing UserCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process UserCreatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncCreated(UserCreatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Created event for User with ID: {}", event.getAggregateId());
    processAsyncCreatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of UserCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleUpdated(UserUpdatedEvent event) {
try {
logger.info("Processing Updated event for User with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Updated":
processUpdatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing UserUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process UserUpdatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncUpdated(UserUpdatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Updated event for User with ID: {}", event.getAggregateId());
    processAsyncUpdatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of UserUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleDeleted(UserDeletedEvent event) {
try {
logger.info("Processing Deleted event for User with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Deleted":
processDeletedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing UserDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process UserDeletedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncDeleted(UserDeletedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Deleted event for User with ID: {}", event.getAggregateId());
    processAsyncDeletedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of UserDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }


    // Event processing methods
    private void processCreatedEvent(UserCreatedEvent event) {
    // TODO: Implement Created event processing logic
    logger.debug("Processing Created event for User ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle creation event - might trigger welcome workflows, notifications, etc.
    handleCreationEvent(event);
    }

    private void processAsyncCreatedEvent(UserCreatedEvent event) {
    // TODO: Implement async Created event processing logic
    logger.debug("Async processing Created event for User ID: {}", event.getAggregateId());
    }

    private void processUpdatedEvent(UserUpdatedEvent event) {
    // TODO: Implement Updated event processing logic
    logger.debug("Processing Updated event for User ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle update event - might trigger validation, sync with external systems, etc.
    handleUpdateEvent(event);
    }

    private void processAsyncUpdatedEvent(UserUpdatedEvent event) {
    // TODO: Implement async Updated event processing logic
    logger.debug("Async processing Updated event for User ID: {}", event.getAggregateId());
    }

    private void processDeletedEvent(UserDeletedEvent event) {
    // TODO: Implement Deleted event processing logic
    logger.debug("Processing Deleted event for User ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle deletion event - might trigger cleanup, archival, notifications, etc.
    handleDeletionEvent(event);
        }

    private void processAsyncDeletedEvent(UserDeletedEvent event) {
    // TODO: Implement async Deleted event processing logic
    logger.debug("Async processing Deleted event for User ID: {}", event.getAggregateId());
    }


    // Helper methods for specific event types
    private void handleCreationEvent(Object event) {
    // TODO: Implement creation-specific logic
    logger.debug("Handling creation event");
    }

    private void handleUpdateEvent(Object event) {
    // TODO: Implement update-specific logic
    logger.debug("Handling update event");
    }

    private void handleDeletionEvent(Object event) {
    // TODO: Implement deletion-specific logic
    logger.debug("Handling deletion event");
    }
    }