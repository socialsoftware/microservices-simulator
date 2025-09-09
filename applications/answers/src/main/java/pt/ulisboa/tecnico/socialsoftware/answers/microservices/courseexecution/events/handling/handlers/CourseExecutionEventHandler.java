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


public abstract class CourseExecutionEventHandler extends EventHandler {
private CourseExecutionRepository courseexecutionRepository;
protected CourseExecutionEventProcessing courseexecutionEventProcessing;

public CourseExecutionEventHandler(CourseExecutionRepository courseexecutionRepository,
CourseExecutionEventProcessing courseexecutionEventProcessing) {
this.courseexecutionRepository = courseexecutionRepository;
this.courseexecutionEventProcessing = courseexecutionEventProcessing;
}

public Set<Integer> getAggregateIds() {
    return
    courseexecutionRepository.findAll().stream().map(CourseExecution::getAggregateId).collect(Collectors.toSet());
    }
    }