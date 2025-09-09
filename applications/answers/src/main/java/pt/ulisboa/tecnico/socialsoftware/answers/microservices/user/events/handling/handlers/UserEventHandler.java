package com.generated.microservices.answers.microservices.user.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.user.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.UserEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class UserEventHandler extends EventHandler {
private UserRepository userRepository;
protected UserEventProcessing userEventProcessing;

public UserEventHandler(UserRepository userRepository,
UserEventProcessing userEventProcessing) {
this.userRepository = userRepository;
this.userEventProcessing = userEventProcessing;
}

public Set<Integer> getAggregateIds() {
    return
    userRepository.findAll().stream().map(User::getAggregateId).collect(Collectors.toSet());
    }
    }