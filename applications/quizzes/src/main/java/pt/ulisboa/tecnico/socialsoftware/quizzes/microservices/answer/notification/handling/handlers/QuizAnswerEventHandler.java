package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing;

import java.util.Set;

public abstract class QuizAnswerEventHandler extends EventHandler {
    private QuizAnswerRepository quizAnswerRepository;
    protected QuizAnswerEventProcessing quizAnswerEventProcessing;

    public QuizAnswerEventHandler(QuizAnswerRepository quizAnswerRepository, QuizAnswerEventProcessing quizAnswerEventProcessing) {
        this.quizAnswerRepository = quizAnswerRepository;
        this.quizAnswerEventProcessing = quizAnswerEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return quizAnswerRepository.findAllAggregateIds();
    }

}
