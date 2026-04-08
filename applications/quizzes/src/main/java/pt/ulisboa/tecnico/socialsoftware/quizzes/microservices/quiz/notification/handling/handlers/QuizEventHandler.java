package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.eventProcessing.QuizEventProcessing;

import java.util.Set;

public abstract class QuizEventHandler extends EventHandler {
    private QuizRepository quizRepository;
    protected QuizEventProcessing quizEventProcessing;

    public QuizEventHandler(QuizRepository quizRepository, QuizEventProcessing quizEventProcessing) {
        this.quizRepository = quizRepository;
        this.quizEventProcessing = quizEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return quizRepository.findAllAggregateIds();
    }

}
