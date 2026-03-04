package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostAuthor;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.events.AuthorUpdatedEvent;

public class PostSubscribesAuthorUpdated extends EventSubscription {
    

    public PostSubscribesAuthorUpdated(PostAuthor postAuthor) {
        super(postAuthor.getAuthorAggregateId(),
                postAuthor.getAuthorVersion(),
                AuthorUpdatedEvent.class.getSimpleName());
        
    }

    public PostSubscribesAuthorUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
