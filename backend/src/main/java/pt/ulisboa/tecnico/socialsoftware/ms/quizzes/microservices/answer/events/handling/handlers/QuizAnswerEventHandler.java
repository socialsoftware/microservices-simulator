package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.eventProcessing.QuizAnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerRepository;

public abstract class QuizAnswerEventHandler extends EventHandler {
    private QuizAnswerRepository quizAnswerRepository;
    protected QuizAnswerEventProcessing quizAnswerEventProcessing;

    public QuizAnswerEventHandler(QuizAnswerRepository quizAnswerRepository, QuizAnswerEventProcessing quizAnswerEventProcessing) {
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizAnswerEventProcessing = quizAnswerEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return quizAnswerRepository.findAll().stream().map(QuizAnswer::getAggregateId).collect(Collectors.toSet());
    }

}
