package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.eventProcessing.PostEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostRepository;

public abstract class PostEventHandler extends EventHandler {
    private PostRepository postRepository;
    protected PostEventProcessing postEventProcessing;

    public PostEventHandler(PostRepository postRepository, PostEventProcessing postEventProcessing) {
        this.postRepository = postRepository;
        this.postEventProcessing = postEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return postRepository.findAll().stream().map(Post::getAggregateId).collect(Collectors.toSet());
    }

}
