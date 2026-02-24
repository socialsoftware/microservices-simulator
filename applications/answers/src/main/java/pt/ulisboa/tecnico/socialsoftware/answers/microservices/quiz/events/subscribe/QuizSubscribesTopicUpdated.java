package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicUpdatedEvent;

public class QuizSubscribesTopicUpdated extends EventSubscription {
    

    public QuizSubscribesTopicUpdated(Quiz quiz) {
        super(quiz.getAggregateId(),
                0,
                TopicUpdatedEvent.class.getSimpleName());
        
    }

    public QuizSubscribesTopicUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
