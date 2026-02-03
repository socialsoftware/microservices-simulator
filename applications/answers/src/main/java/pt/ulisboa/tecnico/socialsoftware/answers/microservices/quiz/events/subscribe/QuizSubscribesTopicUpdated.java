package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.TopicUpdatedEvent;

public class QuizSubscribesTopicUpdated extends EventSubscription {
    

    public QuizSubscribesTopicUpdated( ) {
        super(.getAggregateId(),
                0,
                TopicUpdatedEvent.class.getSimpleName());
        
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
