package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.events.publish.AuthorDeletedEvent;

public class PostSubscribesAuthorDeleted extends EventSubscription {
    public PostSubscribesAuthorDeleted(Post post) {
        super(post.getAggregateId(), 0, AuthorDeletedEvent.class);
    }
}
