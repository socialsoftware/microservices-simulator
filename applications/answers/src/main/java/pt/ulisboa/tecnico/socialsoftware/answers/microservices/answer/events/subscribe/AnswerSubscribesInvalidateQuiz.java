package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.InvalidateQuizEvent;

public class AnswerSubscribesInvalidateQuiz extends EventSubscription {
    public AnswerSubscribesInvalidateQuiz(AnswerQuiz answerquiz) {
        super(answerquiz.getStudentAggregateId(),
                answerquiz.getStudentVersion(),
                InvalidateQuizEvent.class.getSimpleName());
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && ((InvalidateQuizEvent)event).getGetPublisherAggregateId()() == answerQuiz.getQuizAggregateId();
    }
}
