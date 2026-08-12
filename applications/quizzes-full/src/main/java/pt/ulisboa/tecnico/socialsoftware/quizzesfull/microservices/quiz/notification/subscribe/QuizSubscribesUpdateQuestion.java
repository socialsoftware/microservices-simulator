package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion;

public class QuizSubscribesUpdateQuestion extends EventSubscription {

    public QuizSubscribesUpdateQuestion(QuizQuestion question) {
        super(question.getQuestionAggregateId(), question.getQuestionVersion(), UpdateQuestionEvent.class.getSimpleName());
    }

    public QuizSubscribesUpdateQuestion() {}
}
