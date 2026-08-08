package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerExecution;

// Anchored on the execution, so every quiz answer of that execution receives the event. The
// discriminating student check lives in QuizAnswerService, which the sagas profile never reaches
// through subscribesEvent().
public class QuizAnswerSubscribesDisenrollStudentFromCourseExecution extends EventSubscription {
    public QuizAnswerSubscribesDisenrollStudentFromCourseExecution(QuizAnswerExecution execution) {
        super(execution.getExecutionAggregateId(), execution.getExecutionVersion(),
                DisenrollStudentFromCourseExecutionEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesDisenrollStudentFromCourseExecution() {
    }
}
