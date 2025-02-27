package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizCourseExecution;

public class QuizSubscribesDeleteCourseExecution extends EventSubscription {
    public QuizSubscribesDeleteCourseExecution(QuizCourseExecution quizCourseExecution) {
        super(quizCourseExecution.getCourseExecutionAggregateId(),
                quizCourseExecution.getCourseExecutionVersion(),
                DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}