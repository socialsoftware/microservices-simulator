package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;

public interface PostFactory {
    Post createPost(Integer aggregateId, PostDto postDto);
    Post createPostFromExisting(Post existingPost);
    PostDto createPostDto(Post post);
}
