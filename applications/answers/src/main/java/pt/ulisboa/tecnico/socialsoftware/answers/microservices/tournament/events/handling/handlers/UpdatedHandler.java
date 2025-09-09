package com.generated.microservices.answers.microservices.tournament.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.tournament.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.TournamentEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class UpdatedHandler extends TournamentEventHandler {

public UpdatedHandler(TournamentRepository tournamentRepository,
TournamentEventProcessing tournamentEventProcessing) {
super(tournamentRepository, tournamentEventProcessing);
}

@EventListener
public void handleUpdated(TournamentUpdatedEvent event) {
try {
// Handle Updated event for Tournament
tournamentEventProcessing.processUpdated(event);
} catch (Exception e) {
logger.error("Error handling TournamentUpdatedEvent", e);
throw new EventProcessingException("Failed to handle TournamentUpdatedEvent: " + e.getMessage(), e);
}
}
}