package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;

public class QuizAnswerSubscribesAnonymizeStudent extends EventSubscription {
    private Integer studentAggregateId;

    public QuizAnswerSubscribesAnonymizeStudent(QuizAnswer quizAnswer) {
        super(quizAnswer.getAnswerCourseExecution().getCourseExecutionAggregateId(),
                quizAnswer.getAnswerCourseExecution().getCourseExecutionVersion(),
                AnonymizeStudentEvent.class.getSimpleName());

        this.studentAggregateId = quizAnswer.getStudent().getStudentAggregateId();
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && checkAnswerInfo((AnonymizeStudentEvent)event);
    }

    private boolean checkAnswerInfo(AnonymizeStudentEvent anonymizeStudentEvent) {
        return this.studentAggregateId.equals(anonymizeStudentEvent.getStudentAggregateId());
    }


}