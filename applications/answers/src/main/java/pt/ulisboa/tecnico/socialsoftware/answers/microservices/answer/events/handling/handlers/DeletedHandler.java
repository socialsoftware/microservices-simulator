package com.generated.microservices.answers.microservices.answer.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.answer.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.AnswerEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class DeletedHandler extends AnswerEventHandler {

public DeletedHandler(AnswerRepository answerRepository,
AnswerEventProcessing answerEventProcessing) {
super(answerRepository, answerEventProcessing);
}

@EventListener
public void handleDeleted(AnswerDeletedEvent event) {
try {
// Handle Deleted event for Answer
answerEventProcessing.processDeleted(event);
} catch (Exception e) {
logger.error("Error handling AnswerDeletedEvent", e);
throw new EventProcessingException("Failed to handle AnswerDeletedEvent: " + e.getMessage(), e);
}
}
}