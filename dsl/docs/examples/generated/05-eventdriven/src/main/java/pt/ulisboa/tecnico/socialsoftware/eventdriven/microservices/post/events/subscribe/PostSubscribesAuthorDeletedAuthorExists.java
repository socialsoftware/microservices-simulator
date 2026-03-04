package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostAuthor;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.events.AuthorDeletedEvent;


public class PostSubscribesAuthorDeletedAuthorExists extends EventSubscription {
    public PostSubscribesAuthorDeletedAuthorExists(PostAuthor author) {
        super(author.getAuthorAggregateId(),
                author.getAuthorVersion(),
                AuthorDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
