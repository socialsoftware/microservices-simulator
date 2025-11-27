package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuestionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.QuestionRepository;

public abstract class QuestionEventHandler extends EventHandler {
    private QuestionRepository questionRepository;
    protected QuestionEventProcessing questionEventProcessing;

    public QuestionEventHandler(QuestionRepository questionRepository, QuestionEventProcessing questionEventProcessing) {
        this.questionRepository = questionRepository;
        this.questionEventProcessing = questionEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return questionRepository.findAll().stream().map(Question::getAggregateId).collect(Collectors.toSet());
    }

}
