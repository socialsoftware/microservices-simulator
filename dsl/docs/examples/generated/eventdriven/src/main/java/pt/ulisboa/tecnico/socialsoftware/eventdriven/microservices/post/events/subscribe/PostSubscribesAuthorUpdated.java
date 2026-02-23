package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostAuthor;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.events.publish.AuthorUpdatedEvent;

public class PostSubscribesAuthorUpdated extends EventSubscription {
    

    public PostSubscribesAuthorUpdated() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
