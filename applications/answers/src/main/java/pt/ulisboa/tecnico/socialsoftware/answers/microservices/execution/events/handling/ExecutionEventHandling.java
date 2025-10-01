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

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;


@Component
public class ExecutionEventHandling {
private static final Logger logger = LoggerFactory.getLogger(ExecutionEventHandling.class);

private final ExecutionService executionService;
private final ExecutionRepository executionRepository;

public ExecutionEventHandling(ExecutionService executionService, ExecutionRepository
executionRepository) {
this.executionService = executionService;
this.executionRepository = executionRepository;
}

@EventListener
public void handleCreated(ExecutionCreatedEvent event) {
try {
logger.info("Processing Created event for Execution with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Created":
processCreatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing ExecutionCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process ExecutionCreatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncCreated(ExecutionCreatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Created event for Execution with ID: {}", event.getAggregateId());
    processAsyncCreatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of ExecutionCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleUpdated(ExecutionUpdatedEvent event) {
try {
logger.info("Processing Updated event for Execution with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Updated":
processUpdatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing ExecutionUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process ExecutionUpdatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncUpdated(ExecutionUpdatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Updated event for Execution with ID: {}", event.getAggregateId());
    processAsyncUpdatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of ExecutionUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleDeleted(ExecutionDeletedEvent event) {
try {
logger.info("Processing Deleted event for Execution with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Deleted":
processDeletedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing ExecutionDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process ExecutionDeletedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncDeleted(ExecutionDeletedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Deleted event for Execution with ID: {}", event.getAggregateId());
    processAsyncDeletedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of ExecutionDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }


    // Event processing methods
    private void processCreatedEvent(ExecutionCreatedEvent event) {
    // TODO: Implement Created event processing logic
    logger.debug("Processing Created event for Execution ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle creation event - might trigger welcome workflows, notifications, etc.
    handleCreationEvent(event);
    }

    private void processAsyncCreatedEvent(ExecutionCreatedEvent event) {
    // TODO: Implement async Created event processing logic
    logger.debug("Async processing Created event for Execution ID: {}", event.getAggregateId());
    }

    private void processUpdatedEvent(ExecutionUpdatedEvent event) {
    // TODO: Implement Updated event processing logic
    logger.debug("Processing Updated event for Execution ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle update event - might trigger validation, sync with external systems, etc.
    handleUpdateEvent(event);
    }

    private void processAsyncUpdatedEvent(ExecutionUpdatedEvent event) {
    // TODO: Implement async Updated event processing logic
    logger.debug("Async processing Updated event for Execution ID: {}", event.getAggregateId());
    }

    private void processDeletedEvent(ExecutionDeletedEvent event) {
    // TODO: Implement Deleted event processing logic
    logger.debug("Processing Deleted event for Execution ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle deletion event - might trigger cleanup, archival, notifications, etc.
    handleDeletionEvent(event);
        }

    private void processAsyncDeletedEvent(ExecutionDeletedEvent event) {
    // TODO: Implement async Deleted event processing logic
    logger.debug("Async processing Deleted event for Execution ID: {}", event.getAggregateId());
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