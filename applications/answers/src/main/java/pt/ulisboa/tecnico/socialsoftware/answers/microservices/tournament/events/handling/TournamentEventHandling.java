package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;


@Component
public class TournamentEventHandling {
private static final Logger logger = LoggerFactory.getLogger(TournamentEventHandling.class);

private final TournamentService tournamentService;
private final TournamentRepository tournamentRepository;

public TournamentEventHandling(TournamentService tournamentService, TournamentRepository
tournamentRepository) {
this.tournamentService = tournamentService;
this.tournamentRepository = tournamentRepository;
}

@EventListener
public void handleCreated(TournamentCreatedEvent event) {
try {
logger.info("Processing Created event for Tournament with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Created":
processCreatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing TournamentCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process TournamentCreatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncCreated(TournamentCreatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Created event for Tournament with ID: {}", event.getAggregateId());
    processAsyncCreatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of TournamentCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleUpdated(TournamentUpdatedEvent event) {
try {
logger.info("Processing Updated event for Tournament with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Updated":
processUpdatedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing TournamentUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process TournamentUpdatedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncUpdated(TournamentUpdatedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Updated event for Tournament with ID: {}", event.getAggregateId());
    processAsyncUpdatedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of TournamentUpdatedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }

@EventListener
public void handleDeleted(TournamentDeletedEvent event) {
try {
logger.info("Processing Deleted event for Tournament with ID: {}", event.getAggregateId());

switch (event.getEventType()) {
case "Deleted":
processDeletedEvent(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}

} catch (Exception e) {
logger.error("Error processing TournamentDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
throw new EventProcessingException("Failed to process TournamentDeletedEvent: " + e.getMessage(), e);
}
}

@Async
@EventListener
public CompletableFuture<Void> handleAsyncDeleted(TournamentDeletedEvent event) {
    return CompletableFuture.runAsync(() -> {
    try {
    logger.info("Async processing Deleted event for Tournament with ID: {}", event.getAggregateId());
    processAsyncDeletedEvent(event);
    } catch (Exception e) {
    logger.error("Error in async processing of TournamentDeletedEvent for aggregate ID: {}", event.getAggregateId(), e);
    }
    });
    }


    // Event processing methods
    private void processCreatedEvent(TournamentCreatedEvent event) {
    // TODO: Implement Created event processing logic
    logger.debug("Processing Created event for Tournament ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle creation event - might trigger welcome workflows, notifications, etc.
    handleCreationEvent(event);
    }

    private void processAsyncCreatedEvent(TournamentCreatedEvent event) {
    // TODO: Implement async Created event processing logic
    logger.debug("Async processing Created event for Tournament ID: {}", event.getAggregateId());
    }

    private void processUpdatedEvent(TournamentUpdatedEvent event) {
    // TODO: Implement Updated event processing logic
    logger.debug("Processing Updated event for Tournament ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle update event - might trigger validation, sync with external systems, etc.
    handleUpdateEvent(event);
    }

    private void processAsyncUpdatedEvent(TournamentUpdatedEvent event) {
    // TODO: Implement async Updated event processing logic
    logger.debug("Async processing Updated event for Tournament ID: {}", event.getAggregateId());
    }

    private void processDeletedEvent(TournamentDeletedEvent event) {
    // TODO: Implement Deleted event processing logic
    logger.debug("Processing Deleted event for Tournament ID: {}", event.getAggregateId());

    // Example processing based on event type
    // Handle deletion event - might trigger cleanup, archival, notifications, etc.
    handleDeletionEvent(event);
        }

    private void processAsyncDeletedEvent(TournamentDeletedEvent event) {
    // TODO: Implement async Deleted event processing logic
    logger.debug("Async processing Deleted event for Tournament ID: {}", event.getAggregateId());
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