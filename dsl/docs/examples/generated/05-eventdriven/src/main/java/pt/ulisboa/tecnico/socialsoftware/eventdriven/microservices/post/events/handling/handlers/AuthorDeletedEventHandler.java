package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostRepository;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.eventProcessing.PostEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.events.publish.AuthorDeletedEvent;

public class AuthorDeletedEventHandler extends PostEventHandler {
    public AuthorDeletedEventHandler(PostRepository postRepository, PostEventProcessing postEventProcessing) {
        super(postRepository, postEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.postEventProcessing.processAuthorDeletedEvent(subscriberAggregateId, (AuthorDeletedEvent) event);
    }
}
