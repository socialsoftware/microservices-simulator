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

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.UserEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class CreatedHandler extends UserEventHandler {

public CreatedHandler(UserRepository userRepository,
UserEventProcessing userEventProcessing) {
super(userRepository, userEventProcessing);
}

@EventListener
public void handleCreated(UserCreatedEvent event) {
try {
// Handle Created event for User
userEventProcessing.processCreated(event);
} catch (Exception e) {
logger.error("Error handling UserCreatedEvent", e);
throw new EventProcessingException("Failed to handle UserCreatedEvent: " + e.getMessage(), e);
}
}
}