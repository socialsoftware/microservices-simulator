package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.AnswerCourseExecution;

public class QuizAnswerSubscribesRemoveCourseExecution extends EventSubscription {
    public QuizAnswerSubscribesRemoveCourseExecution(AnswerCourseExecution answerCourseExecution) {
        super(answerCourseExecution.getCourseExecutionAggregateId(),
                answerCourseExecution.getCourseExecutionVersion(),
                DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesRemoveCourseExecution() {}

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }


}