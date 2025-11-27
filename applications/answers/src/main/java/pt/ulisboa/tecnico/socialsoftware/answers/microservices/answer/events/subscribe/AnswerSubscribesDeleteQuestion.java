package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.DeleteQuestionEvent;

public class AnswerSubscribesDeleteQuestion extends EventSubscription {
    public AnswerSubscribesDeleteQuestion(AnswerQuiz answerquiz) {
        super(answerquiz.getStudentAggregateId(),
                answerquiz.getStudentVersion(),
                DeleteQuestionEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && true;
    }
}
