package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicDeletedEvent;

public class QuizSubscribesTopicDeleted extends EventSubscription {
    

    public QuizSubscribesTopicDeleted(Quiz quiz) {
        super(quiz.getAggregateId(),
                0,
                TopicDeletedEvent.class.getSimpleName());
        
    }

    public QuizSubscribesTopicDeleted() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
