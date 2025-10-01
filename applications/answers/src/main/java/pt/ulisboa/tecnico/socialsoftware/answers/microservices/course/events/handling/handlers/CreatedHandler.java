package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.CourseEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class CreatedHandler extends CourseEventHandler {

public CreatedHandler(CourseRepository courseRepository,
CourseEventProcessing courseEventProcessing) {
super(courseRepository, courseEventProcessing);
}

@EventListener
public void handleCreated(CourseCreatedEvent event) {
try {
// Handle Created event for Course
courseEventProcessing.processCreated(event);
} catch (Exception e) {
logger.error("Error handling CourseCreatedEvent", e);
throw new EventProcessingException("Failed to handle CourseCreatedEvent: " + e.getMessage(), e);
}
}
}