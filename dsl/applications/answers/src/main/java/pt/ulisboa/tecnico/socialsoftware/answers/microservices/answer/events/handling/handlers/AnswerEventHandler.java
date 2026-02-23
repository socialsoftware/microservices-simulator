package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.AnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerRepository;

public abstract class AnswerEventHandler extends EventHandler {
    private AnswerRepository answerRepository;
    protected AnswerEventProcessing answerEventProcessing;

    public AnswerEventHandler(AnswerRepository answerRepository, AnswerEventProcessing answerEventProcessing) {
        this.answerRepository = answerRepository;
        this.answerEventProcessing = answerEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return answerRepository.findAll().stream().map(Answer::getAggregateId).collect(Collectors.toSet());
    }

}
