package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.coordination.eventProcessing.PostEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostRepository;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.handling.handlers.AuthorUpdatedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.events.AuthorUpdatedEvent;

@Component
public class PostEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private PostEventProcessing postEventProcessing;
    @Autowired
    private PostRepository postRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleAuthorUpdatedEventEvents() {
        eventApplicationService.handleSubscribedEvent(AuthorUpdatedEvent.class,
                new AuthorUpdatedEventHandler(postRepository, postEventProcessing));
    }

}