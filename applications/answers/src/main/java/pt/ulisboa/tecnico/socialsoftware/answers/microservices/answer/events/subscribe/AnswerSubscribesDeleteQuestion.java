package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.DeleteQuestionEvent;

public class AnswerSubscribesDeleteQuestion extends EventSubscription {
    private final Answer answer;

    public AnswerSubscribesDeleteQuestion(Answer answer) {
        super(answer.getExecution().getExecutionAggregateId(),
                answer.getExecution().getExecutionVersion(),
                DeleteQuestionEvent.class.getSimpleName());
        this.answer = answer;
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event) && true;
    }
}
