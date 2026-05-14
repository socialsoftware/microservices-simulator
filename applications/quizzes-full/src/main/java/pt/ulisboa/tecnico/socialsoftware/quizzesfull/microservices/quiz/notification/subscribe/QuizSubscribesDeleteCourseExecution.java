package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizExecution;

public class QuizSubscribesDeleteCourseExecution extends EventSubscription {

    public QuizSubscribesDeleteCourseExecution(QuizExecution execution) {
        super(execution.getExecutionAggregateId(), execution.getExecutionVersion(), DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public QuizSubscribesDeleteCourseExecution() {}
}
