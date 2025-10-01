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

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.ExecutionEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class CreatedHandler extends ExecutionEventHandler {

public CreatedHandler(ExecutionRepository executionRepository,
ExecutionEventProcessing executionEventProcessing) {
super(executionRepository, executionEventProcessing);
}

@EventListener
public void handleCreated(ExecutionCreatedEvent event) {
try {
// Handle Created event for Execution
executionEventProcessing.processCreated(event);
} catch (Exception e) {
logger.error("Error handling ExecutionCreatedEvent", e);
throw new EventProcessingException("Failed to handle ExecutionCreatedEvent: " + e.getMessage(), e);
}
}
}