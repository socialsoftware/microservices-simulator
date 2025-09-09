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
public class DeletedHandler extends TopicEventHandler {

public DeletedHandler(TopicRepository topicRepository,
TopicEventProcessing topicEventProcessing) {
super(topicRepository, topicEventProcessing);
}

@EventListener
public void handleDeleted(TopicDeletedEvent event) {
try {
// Handle Deleted event for Topic
topicEventProcessing.processDeleted(event);
} catch (Exception e) {
logger.error("Error handling TopicDeletedEvent", e);
throw new EventProcessingException("Failed to handle TopicDeletedEvent: " + e.getMessage(), e);
}
}
}