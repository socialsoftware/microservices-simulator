package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionUpdatedEvent;

public class AnswerSubscribesQuestionUpdated extends EventSubscription {
    

    public AnswerSubscribesQuestionUpdated() {
        // Parameterless constructor for simple subscriptions
        // Event matching is handled by the framework
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
