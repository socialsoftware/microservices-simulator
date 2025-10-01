package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;


@Component
public class CourseEventHandling {
private static final Logger logger = LoggerFactory.getLogger(CourseEventHandling.class);

private final CourseService courseService;
private final CourseRepository courseRepository;

public CourseEventHandling(CourseService courseService, CourseRepository
courseRepository) {
this.courseService = courseService;
this.courseRepository = courseRepository;
}

@EventListener
public void handleCreated(CourseCreatedEvent event) {
try {
logger.info("Processing Created event for Course with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Created":
processCreatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing CourseCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process CourseCreatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncCreated(CourseCreatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Created event for Course with ID: {}", event.getAggregateId());
    processAsyncCreatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of CourseCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleUpdated(CourseUpdatedEvent event) {
try {
logger.info("Processing Updated event for Course with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Updated":
processUpdatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing CourseUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process CourseUpdatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncUpdated(CourseUpdatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Updated event for Course with ID: {}", event.getAggregateId());
    processAsyncUpdatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of CourseUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleDeleted(CourseDeletedEvent event) {
try {
logger.info("Processing Deleted event for Course with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Deleted":
processDeletedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing CourseDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process CourseDeletedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncDeleted(CourseDeletedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Deleted event for Course with ID: {}", event.getAggregateId());
    processAsyncDeletedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of CourseDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }


    // Event processing methods
    private void processCreatedEvent(CourseCreatedEvent event) {
    // TODO: Implement Created event processing logic
    logger.debug("Processing Created event for Course ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle creation event - might trigger welcome workflows, notifications, etc.
    handleCreationEvent(event);
    }

    private void processAsyncCreatedEvent(CourseCreatedEvent event) {
    // TODO: Implement async Created event processing logic
    logger.debug("Async processing Created event for Course ID: {}", event.getAggregateId());
    }

    private void processUpdatedEvent(CourseUpdatedEvent event) {
    // TODO: Implement Updated event processing logic
    logger.debug("Processing Updated event for Course ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle update event - might trigger validation, sync with external systems, etc.
    handleUpdateEvent(event);
    }

    private void processAsyncUpdatedEvent(CourseUpdatedEvent event) {
    // TODO: Implement async Updated event processing logic
    logger.debug("Async processing Updated event for Course ID: {}", event.getAggregateId());
    }

    private void processDeletedEvent(CourseDeletedEvent event) {
    // TODO: Implement Deleted event processing logic
    logger.debug("Processing Deleted event for Course ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle deletion event - might trigger cleanup, archival, notifications, etc.
    handleDeletionEvent(event);
        }

    private void processAsyncDeletedEvent(CourseDeletedEvent event) {
    // TODO: Implement async Deleted event processing logic
    logger.debug("Async processing Deleted event for Course ID: {}", event.getAggregateId());
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