package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostFactory;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.SagaPost;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.dtos.SagaPostDto;

@Service
@Profile("sagas")
public class SagasPostFactory implements PostFactory {
    @Override
    public Post createPost(Integer aggregateId, PostDto postDto) {
        return new SagaPost(aggregateId, postDto);
    }

    @Override
    public Post createPostFromExisting(Post existingPost) {
        return new SagaPost((SagaPost) existingPost);
    }

    @Override
    public PostDto createPostDto(Post post) {
        return new SagaPostDto(post);
    }
}