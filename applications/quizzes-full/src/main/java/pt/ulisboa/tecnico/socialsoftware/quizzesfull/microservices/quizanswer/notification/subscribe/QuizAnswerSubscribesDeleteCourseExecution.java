package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;

public class QuizAnswerSubscribesDeleteCourseExecution extends EventSubscription {

    public QuizAnswerSubscribesDeleteCourseExecution(QuizAnswer quizAnswer) {
        super(quizAnswer.getExecutionAggregateId(), quizAnswer.getExecutionVersion(), DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesDeleteCourseExecution() {}
}
