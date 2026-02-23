package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostAuthorDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.publish.PostDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.publish.PostUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.publish.PostAuthorDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.events.publish.PostAuthorUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.exception.EventdrivenException;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi.requestDtos.CreatePostRequestDto;


@Service
@Transactional
public class PostService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostFactory postFactory;

    public PostService() {}

    public PostDto createPost(CreatePostRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            PostDto postDto = new PostDto();
            postDto.setTitle(createRequest.getTitle());
            postDto.setContent(createRequest.getContent());
            postDto.setPublishedAt(createRequest.getPublishedAt());
            if (createRequest.getAuthor() != null) {
                PostAuthorDto authorDto = new PostAuthorDto();
                authorDto.setAggregateId(createRequest.getAuthor().getAggregateId());
                authorDto.setVersion(createRequest.getAuthor().getVersion());
                authorDto.setState(createRequest.getAuthor().getState() != null ? createRequest.getAuthor().getState().name() : null);
                postDto.setAuthor(authorDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Post post = postFactory.createPost(aggregateId, postDto);
            unitOfWorkService.registerChanged(post, unitOfWork);
            return postFactory.createPostDto(post);
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error creating post: " + e.getMessage());
        }
    }

    public PostDto getPostById(Integer id, UnitOfWork unitOfWork) {
        try {
            Post post = (Post) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return postFactory.createPostDto(post);
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error retrieving post: " + e.getMessage());
        }
    }

    public List<PostDto> getAllPosts(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = postRepository.findAll().stream()
                .map(Post::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Post) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(postFactory::createPostDto)
                .collect(Collectors.toList());
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error retrieving post: " + e.getMessage());
        }
    }

    public PostDto updatePost(PostDto postDto, UnitOfWork unitOfWork) {
        try {
            Integer id = postDto.getAggregateId();
            Post oldPost = (Post) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Post newPost = postFactory.createPostFromExisting(oldPost);
            if (postDto.getTitle() != null) {
                newPost.setTitle(postDto.getTitle());
            }
            if (postDto.getContent() != null) {
                newPost.setContent(postDto.getContent());
            }
            if (postDto.getPublishedAt() != null) {
                newPost.setPublishedAt(postDto.getPublishedAt());
            }

            unitOfWorkService.registerChanged(newPost, unitOfWork);            PostUpdatedEvent event = new PostUpdatedEvent(newPost.getAggregateId(), newPost.getTitle(), newPost.getContent(), newPost.getPublishedAt());
            event.setPublisherAggregateVersion(newPost.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return postFactory.createPostDto(newPost);
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error updating post: " + e.getMessage());
        }
    }

    public void deletePost(Integer id, UnitOfWork unitOfWork) {
        try {
            Post oldPost = (Post) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Post newPost = postFactory.createPostFromExisting(oldPost);
            newPost.remove();
            unitOfWorkService.registerChanged(newPost, unitOfWork);            unitOfWorkService.registerEvent(new PostDeletedEvent(newPost.getAggregateId()), unitOfWork);
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error deleting post: " + e.getMessage());
        }
    }




    public Post handleAuthorUpdatedEvent(Integer aggregateId, Integer authorAggregateId, Integer authorVersion, String authorName, UnitOfWork unitOfWork) {
        try {
            Post oldPost = (Post) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Post newPost = postFactory.createPostFromExisting(oldPost);



            unitOfWorkService.registerChanged(newPost, unitOfWork);

        unitOfWorkService.registerEvent(
            new PostAuthorUpdatedEvent(
                    newPost.getAggregateId(),
                    authorAggregateId,
                    authorVersion,
                    authorName
            ),
            unitOfWork
        );

            return newPost;
        } catch (EventdrivenException e) {
            throw e;
        } catch (Exception e) {
            throw new EventdrivenException("Error handling AuthorUpdatedEvent post: " + e.getMessage());
        }
    }




}