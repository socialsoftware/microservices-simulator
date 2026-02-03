package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicDeletedEvent;

public class QuizSubscribesTopicDeleted extends EventSubscription {
    

    public QuizSubscribesTopicDeleted() {
        // Parameterless constructor for simple subscriptions
        // Event matching is handled by the framework
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
