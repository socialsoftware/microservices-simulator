package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing;

public abstract class QuizAnswerEventHandler extends EventHandler {
    protected QuizAnswerEventProcessing quizAnswerEventProcessing;

    public QuizAnswerEventHandler(QuizAnswerRepository quizAnswerRepository, QuizAnswerEventProcessing quizAnswerEventProcessing) {
        super(quizAnswerRepository);
        this.quizAnswerEventProcessing = quizAnswerEventProcessing;
    }

}
