package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.functionalities.PostFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi.requestDtos.CreatePostRequestDto;

@RestController
public class PostController {
    @Autowired
    private PostFunctionalities postFunctionalities;

    @PostMapping("/posts/create")
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto createPost(@RequestBody CreatePostRequestDto createRequest) {
        return postFunctionalities.createPost(createRequest);
    }

    @GetMapping("/posts/{postAggregateId}")
    public PostDto getPostById(@PathVariable Integer postAggregateId) {
        return postFunctionalities.getPostById(postAggregateId);
    }

    @PutMapping("/posts")
    public PostDto updatePost(@RequestBody PostDto postDto) {
        return postFunctionalities.updatePost(postDto);
    }

    @DeleteMapping("/posts/{postAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Integer postAggregateId) {
        postFunctionalities.deletePost(postAggregateId);
    }

    @GetMapping("/posts")
    public List<PostDto> getAllPosts() {
        return postFunctionalities.getAllPosts();
    }
}
