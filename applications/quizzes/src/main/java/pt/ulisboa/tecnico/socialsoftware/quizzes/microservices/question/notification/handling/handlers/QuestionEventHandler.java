package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.eventProcessing.QuestionEventProcessing;

public abstract class QuestionEventHandler extends EventHandler {
    protected QuestionEventProcessing questionEventProcessing;

    public QuestionEventHandler(QuestionRepository questionRepository, QuestionEventProcessing questionEventProcessing) {
        super(questionRepository);
        this.questionEventProcessing = questionEventProcessing;
    }

}
