package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerExecution;

public class QuizAnswerSubscribesDeleteCourseExecution extends EventSubscription {
    public QuizAnswerSubscribesDeleteCourseExecution(QuizAnswerExecution execution) {
        super(execution.getExecutionAggregateId(), execution.getExecutionVersion(),
                DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesDeleteCourseExecution() {
    }
}
