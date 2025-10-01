package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuizEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class UpdatedHandler extends QuizEventHandler {

public UpdatedHandler(QuizRepository quizRepository,
QuizEventProcessing quizEventProcessing) {
super(quizRepository, quizEventProcessing);
}

@EventListener
public void handleUpdated(QuizUpdatedEvent event) {
try {
// Handle Updated event for Quiz
quizEventProcessing.processUpdated(event);
} catch (Exception e) {
logger.error("Error handling QuizUpdatedEvent", e);
throw new EventProcessingException("Failed to handle QuizUpdatedEvent: " + e.getMessage(), e);
}
}
}