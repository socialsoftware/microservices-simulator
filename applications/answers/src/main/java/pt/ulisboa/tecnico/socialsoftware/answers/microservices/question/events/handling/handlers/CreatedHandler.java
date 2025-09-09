package com.generated.microservices.answers.microservices.question.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.question.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuestionEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class CreatedHandler extends QuestionEventHandler {

public CreatedHandler(QuestionRepository questionRepository,
QuestionEventProcessing questionEventProcessing) {
super(questionRepository, questionEventProcessing);
}

@EventListener
public void handleCreated(QuestionCreatedEvent event) {
try {
// Handle Created event for Question
questionEventProcessing.processCreated(event);
} catch (Exception e) {
logger.error("Error handling QuestionCreatedEvent", e);
throw new EventProcessingException("Failed to handle QuestionCreatedEvent: " + e.getMessage(), e);
}
}
}