package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;

public class QuizAnswerSubscribesDisenrollStudentFromCourseExecution extends EventSubscription {

    public QuizAnswerSubscribesDisenrollStudentFromCourseExecution(QuizAnswer quizAnswer) {
        super(quizAnswer.getExecutionAggregateId(), quizAnswer.getExecutionVersion(),
                DisenrollStudentFromCourseExecutionEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesDisenrollStudentFromCourseExecution() {}
}
