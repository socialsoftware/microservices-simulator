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
public class DeletedHandler extends ExecutionEventHandler {

public DeletedHandler(ExecutionRepository executionRepository,
ExecutionEventProcessing executionEventProcessing) {
super(executionRepository, executionEventProcessing);
}

@EventListener
public void handleDeleted(ExecutionDeletedEvent event) {
try {
// Handle Deleted event for Execution
executionEventProcessing.processDeleted(event);
} catch (Exception e) {
logger.error("Error handling ExecutionDeletedEvent", e);
throw new EventProcessingException("Failed to handle ExecutionDeletedEvent: " + e.getMessage(), e);
}
}
}