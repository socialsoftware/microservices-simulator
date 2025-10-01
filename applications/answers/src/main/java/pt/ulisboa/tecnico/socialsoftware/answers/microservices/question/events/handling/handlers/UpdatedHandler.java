package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuestionEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class UpdatedHandler extends QuestionEventHandler {

public UpdatedHandler(QuestionRepository questionRepository,
QuestionEventProcessing questionEventProcessing) {
super(questionRepository, questionEventProcessing);
}

@EventListener
public void handleUpdated(QuestionUpdatedEvent event) {
try {
// Handle Updated event for Question
questionEventProcessing.processUpdated(event);
} catch (Exception e) {
logger.error("Error handling QuestionUpdatedEvent", e);
throw new EventProcessingException("Failed to handle QuestionUpdatedEvent: " + e.getMessage(), e);
}
}
}