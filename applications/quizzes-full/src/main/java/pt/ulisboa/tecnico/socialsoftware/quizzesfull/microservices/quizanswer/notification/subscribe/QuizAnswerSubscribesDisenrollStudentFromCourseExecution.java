package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;

public class QuizAnswerSubscribesDisenrollStudentFromCourseExecution extends EventSubscription {

    private Integer userAggregateId;

    public QuizAnswerSubscribesDisenrollStudentFromCourseExecution(QuizAnswer quizAnswer) {
        super(quizAnswer.getExecutionAggregateId(), quizAnswer.getExecutionVersion(),
                DisenrollStudentFromCourseExecutionEvent.class.getSimpleName());
        this.userAggregateId = quizAnswer.getUserAggregateId();
    }

    public QuizAnswerSubscribesDisenrollStudentFromCourseExecution() {}

    @Override
    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) &&
                userAggregateId != null &&
                userAggregateId.equals(((DisenrollStudentFromCourseExecutionEvent) event).getUserId());
    }
}
