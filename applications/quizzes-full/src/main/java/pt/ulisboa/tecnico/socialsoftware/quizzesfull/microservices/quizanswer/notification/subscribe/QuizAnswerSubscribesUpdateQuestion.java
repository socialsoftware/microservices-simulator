package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuestionAnswer;

public class QuizAnswerSubscribesUpdateQuestion extends EventSubscription {

    public QuizAnswerSubscribesUpdateQuestion(QuestionAnswer questionAnswer) {
        super(questionAnswer.getQuestionAggregateId(), questionAnswer.getQuestionVersion(), UpdateQuestionEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesUpdateQuestion() {}
}
