package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.ExecutionEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class ExecutionEventHandler extends EventHandler {
private ExecutionRepository executionRepository;
protected ExecutionEventProcessing executionEventProcessing;

public ExecutionEventHandler(ExecutionRepository executionRepository,
ExecutionEventProcessing executionEventProcessing) {
this.executionRepository = executionRepository;
this.executionEventProcessing = executionEventProcessing;
}

public Set<Integer> getAggregateIds() {
    return
    executionRepository.findAll().stream().map(Execution::getAggregateId).collect(Collectors.toSet());
    }
    }