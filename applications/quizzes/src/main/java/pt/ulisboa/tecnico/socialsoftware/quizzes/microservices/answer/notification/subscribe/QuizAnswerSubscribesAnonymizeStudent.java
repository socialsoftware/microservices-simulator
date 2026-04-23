package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswer;

public class QuizAnswerSubscribesAnonymizeStudent extends EventSubscription {
    private Integer studentAggregateId;

    public QuizAnswerSubscribesAnonymizeStudent(QuizAnswer quizAnswer) {
        super(quizAnswer.getAnswerCourseExecution().getCourseExecutionAggregateId(),
                quizAnswer.getAnswerCourseExecution().getCourseExecutionVersion(),
                AnonymizeStudentEvent.class.getSimpleName());

        this.studentAggregateId = quizAnswer.getStudent().getStudentAggregateId();
    }

    public QuizAnswerSubscribesAnonymizeStudent() {}

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && checkAnswerInfo((AnonymizeStudentEvent)event);
    }

    private boolean checkAnswerInfo(AnonymizeStudentEvent anonymizeStudentEvent) {
        return this.studentAggregateId.equals(anonymizeStudentEvent.getStudentAggregateId());
    }


}