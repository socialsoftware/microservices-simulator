package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;


@Component
public class TopicEventHandling {
private static final Logger logger = LoggerFactory.getLogger(TopicEventHandling.class);

private final TopicService topicService;
private final TopicRepository topicRepository;

public TopicEventHandling(TopicService topicService, TopicRepository
topicRepository) {
this.topicService = topicService;
this.topicRepository = topicRepository;
}

@EventListener
public void handleCreated(TopicCreatedEvent event) {
try {
logger.info("Processing Created event for Topic with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Created":
processCreatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing TopicCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process TopicCreatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncCreated(TopicCreatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Created event for Topic with ID: {}", event.getAggregateId());
    processAsyncCreatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of TopicCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleUpdated(TopicUpdatedEvent event) {
try {
logger.info("Processing Updated event for Topic with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Updated":
processUpdatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing TopicUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process TopicUpdatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncUpdated(TopicUpdatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Updated event for Topic with ID: {}", event.getAggregateId());
    processAsyncUpdatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of TopicUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleDeleted(TopicDeletedEvent event) {
try {
logger.info("Processing Deleted event for Topic with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Deleted":
processDeletedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing TopicDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process TopicDeletedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncDeleted(TopicDeletedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Deleted event for Topic with ID: {}", event.getAggregateId());
    processAsyncDeletedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of TopicDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }


    // Event processing methods
    private void processCreatedEvent(TopicCreatedEvent event) {
    // TODO: Implement Created event processing logic
    logger.debug("Processing Created event for Topic ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle creation event - might trigger welcome workflows, notifications, etc.
    handleCreationEvent(event);
    }

    private void processAsyncCreatedEvent(TopicCreatedEvent event) {
    // TODO: Implement async Created event processing logic
    logger.debug("Async processing Created event for Topic ID: {}", event.getAggregateId());
    }

    private void processUpdatedEvent(TopicUpdatedEvent event) {
    // TODO: Implement Updated event processing logic
    logger.debug("Processing Updated event for Topic ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle update event - might trigger validation, sync with external systems, etc.
    handleUpdateEvent(event);
    }

    private void processAsyncUpdatedEvent(TopicUpdatedEvent event) {
    // TODO: Implement async Updated event processing logic
    logger.debug("Async processing Updated event for Topic ID: {}", event.getAggregateId());
    }

    private void processDeletedEvent(TopicDeletedEvent event) {
    // TODO: Implement Deleted event processing logic
    logger.debug("Processing Deleted event for Topic ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle deletion event - might trigger cleanup, archival, notifications, etc.
    handleDeletionEvent(event);
        }

    private void processAsyncDeletedEvent(TopicDeletedEvent event) {
    // TODO: Implement async Deleted event processing logic
    logger.debug("Async processing Deleted event for Topic ID: {}", event.getAggregateId());
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