package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.AnswerCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;

public class QuizAnswerSubscribesRemoveCourseExecution extends EventSubscription {
    public QuizAnswerSubscribesRemoveCourseExecution(AnswerCourseExecution answerCourseExecution) {
        super(answerCourseExecution.getCourseExecutionAggregateId(),
                answerCourseExecution.getCourseExecutionVersion(),
                DeleteCourseExecutionEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }


}