package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizExecution;

public class QuizSubscribesDeleteCourseExecution extends EventSubscription {
    public QuizSubscribesDeleteCourseExecution(QuizExecution execution) {
        super(execution.getExecutionAggregateId(), execution.getExecutionVersion(),
                DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public QuizSubscribesDeleteCourseExecution() {
    }
}
