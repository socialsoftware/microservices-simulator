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
public class CreatedHandler extends QuizEventHandler {

public CreatedHandler(QuizRepository quizRepository,
QuizEventProcessing quizEventProcessing) {
super(quizRepository, quizEventProcessing);
}

@EventListener
public void handleCreated(QuizCreatedEvent event) {
try {
// Handle Created event for Quiz
quizEventProcessing.processCreated(event);
} catch (Exception e) {
logger.error("Error handling QuizCreatedEvent", e);
throw new EventProcessingException("Failed to handle QuizCreatedEvent: " + e.getMessage(), e);
}
}
}