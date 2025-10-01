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

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;


@Component
public class CreatedSubscription {
private static final Logger logger = LoggerFactory.getLogger(CreatedSubscription.class);

private final TournamentService tournamentService;

public CreatedSubscription(TournamentService tournamentService) {
this.tournamentService = tournamentService;
}

@EventListener
@Async
public void handleCreated(TournamentCreatedEvent event) {
try {
logger.info("Handling Created event for Tournament with ID: {}", event.getAggregateId());

// Process the Created event
processCreated(event);

} catch (Exception e) {
logger.error("Error handling TournamentCreatedEvent for aggregate ID: {}", event.getAggregateId(), e);
// Consider implementing retry logic or dead letter queue
}
}

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleAfterCommitCreated(TournamentCreatedEvent event) {
try {
logger.info("Post-commit handling Created event for Tournament with ID: {}",
event.getAggregateId());

// Post-transaction processing
postProcessCreated(event);

} catch (Exception e) {
logger.error("Error in post-commit handling of TournamentCreatedEvent for aggregate ID: {}",
event.getAggregateId(), e);
}
}

private void processCreated(TournamentCreatedEvent event) {
// TODO: Implement Created event processing logic
switch (event.getEventType()) {
case "Created":
handleCreatedLogic(event);
break;
default:
logger.warn("Unknown event type: {}", event.getEventType());
}
}

private void postProcessCreated(TournamentCreatedEvent event) {
// TODO: Implement post-transaction Created event processing
// This could include:
// - Sending notifications
// - Updating external systems
// - Triggering workflows
logger.debug("Post-processing Created event completed for aggregate ID: {}", event.getAggregateId());
}

private void handleCreatedLogic(TournamentCreatedEvent event) {
// TODO: Implement specific Created business logic
logger.debug("Processing Created logic for aggregate ID: {}", event.getAggregateId());
}
}