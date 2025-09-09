package com.generated.microservices.answers.microservices.courseexecution.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.courseexecution.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.CourseExecutionEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class CreatedHandler extends CourseExecutionEventHandler {

public CreatedHandler(CourseExecutionRepository courseexecutionRepository,
CourseExecutionEventProcessing courseexecutionEventProcessing) {
super(courseexecutionRepository, courseexecutionEventProcessing);
}

@EventListener
public void handleCreated(CourseExecutionCreatedEvent event) {
try {
// Handle Created event for CourseExecution
courseexecutionEventProcessing.processCreated(event);
} catch (Exception e) {
logger.error("Error handling CourseExecutionCreatedEvent", e);
throw new EventProcessingException("Failed to handle CourseExecutionCreatedEvent: " + e.getMessage(), e);
}
}
}