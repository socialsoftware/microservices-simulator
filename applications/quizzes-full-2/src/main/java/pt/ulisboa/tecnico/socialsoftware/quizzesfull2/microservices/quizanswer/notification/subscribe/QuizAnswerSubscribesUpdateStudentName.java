package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerStudent;

public class QuizAnswerSubscribesUpdateStudentName extends EventSubscription {
    public QuizAnswerSubscribesUpdateStudentName(QuizAnswerStudent student) {
        super(student.getUserAggregateId(), student.getUserVersion(),
                UpdateStudentNameEvent.class.getSimpleName());
    }

    public QuizAnswerSubscribesUpdateStudentName() {
    }
}
