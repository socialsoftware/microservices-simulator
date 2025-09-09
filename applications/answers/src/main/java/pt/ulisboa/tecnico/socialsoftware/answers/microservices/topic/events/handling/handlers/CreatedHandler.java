package com.generated.microservices.answers.microservices.topic.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.topic.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.TopicEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class CreatedHandler extends TopicEventHandler {

public CreatedHandler(TopicRepository topicRepository,
TopicEventProcessing topicEventProcessing) {
super(topicRepository, topicEventProcessing);
}

@EventListener
public void handleCreated(TopicCreatedEvent event) {
try {
// Handle Created event for Topic
topicEventProcessing.processCreated(event);
} catch (Exception e) {
logger.error("Error handling TopicCreatedEvent", e);
throw new EventProcessingException("Failed to handle TopicCreatedEvent: " + e.getMessage(), e);
}
}
}