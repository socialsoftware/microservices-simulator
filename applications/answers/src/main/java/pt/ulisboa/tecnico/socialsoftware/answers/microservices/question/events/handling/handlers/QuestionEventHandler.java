package com.generated.microservices.answers.microservices.question.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.question.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuestionEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class QuestionEventHandler extends EventHandler {
private QuestionRepository questionRepository;
protected QuestionEventProcessing questionEventProcessing;

public QuestionEventHandler(QuestionRepository questionRepository,
QuestionEventProcessing questionEventProcessing) {
this.questionRepository = questionRepository;
this.questionEventProcessing = questionEventProcessing;
}

public Set<Integer> getAggregateIds() {
    return
    questionRepository.findAll().stream().map(Question::getAggregateId).collect(Collectors.toSet());
    }
    }