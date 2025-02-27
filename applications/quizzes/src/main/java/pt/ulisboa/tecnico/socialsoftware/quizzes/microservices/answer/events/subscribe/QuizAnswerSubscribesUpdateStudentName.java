package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;

public class QuizAnswerSubscribesUpdateStudentName extends EventSubscription {
    private Integer studentAggregateId;

    public QuizAnswerSubscribesUpdateStudentName(QuizAnswer quizAnswer) {
        super(quizAnswer.getAnswerCourseExecution().getCourseExecutionAggregateId(),
                quizAnswer.getAnswerCourseExecution().getCourseExecutionVersion(),
                UpdateStudentNameEvent.class.getSimpleName());

        this.studentAggregateId = quizAnswer.getStudent().getStudentAggregateId();
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && checkAnswerInfo((UpdateStudentNameEvent)event);
    }

    private boolean checkAnswerInfo(UpdateStudentNameEvent updateStudentNameEvent) {
        return this.studentAggregateId.equals(updateStudentNameEvent.getStudentAggregateId());
    }


}