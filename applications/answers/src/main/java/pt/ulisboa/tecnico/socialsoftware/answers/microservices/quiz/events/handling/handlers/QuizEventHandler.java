package com.generated.microservices.answers.microservices.quiz.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.generated.microservices.answers.microservices.quiz.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuizEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class QuizEventHandler extends EventHandler {
private QuizRepository quizRepository;
protected QuizEventProcessing quizEventProcessing;

public QuizEventHandler(QuizRepository quizRepository,
QuizEventProcessing quizEventProcessing) {
this.quizRepository = quizRepository;
this.quizEventProcessing = quizEventProcessing;
}

public Set<Integer> getAggregateIds() {
    return
    quizRepository.findAll().stream().map(Quiz::getAggregateId).collect(Collectors.toSet());
    }
    }