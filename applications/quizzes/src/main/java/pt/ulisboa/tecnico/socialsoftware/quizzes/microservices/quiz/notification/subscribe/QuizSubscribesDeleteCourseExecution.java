package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizCourseExecution;

public class QuizSubscribesDeleteCourseExecution extends EventSubscription {
    public QuizSubscribesDeleteCourseExecution(QuizCourseExecution quizCourseExecution) {
        super(quizCourseExecution.getCourseExecutionAggregateId(),
                quizCourseExecution.getCourseExecutionVersion(),
                DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public QuizSubscribesDeleteCourseExecution() {}

    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}