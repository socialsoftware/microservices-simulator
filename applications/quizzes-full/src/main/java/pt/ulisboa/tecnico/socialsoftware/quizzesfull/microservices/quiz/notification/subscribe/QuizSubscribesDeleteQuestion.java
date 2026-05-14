package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion;

public class QuizSubscribesDeleteQuestion extends EventSubscription {

    public QuizSubscribesDeleteQuestion(QuizQuestion question) {
        super(question.getQuestionAggregateId(), question.getQuestionVersion(), DeleteQuestionEvent.class.getSimpleName());
    }

    public QuizSubscribesDeleteQuestion() {}
}
