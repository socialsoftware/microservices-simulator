package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.exception.EventdrivenErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.exception.EventdrivenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.post.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service.PostService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi.requestDtos.CreatePostRequestDto;
import java.util.List;

@Service
public class PostFunctionalities {
    @Autowired
    private PostService postService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public PostDto createPost(CreatePostRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreatePostFunctionalitySagas createPostFunctionalitySagas = new CreatePostFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, postService, createRequest);
                createPostFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createPostFunctionalitySagas.getCreatedPostDto();
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public PostDto getPostById(Integer postAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetPostByIdFunctionalitySagas getPostByIdFunctionalitySagas = new GetPostByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, postService, postAggregateId);
                getPostByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getPostByIdFunctionalitySagas.getPostDto();
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public PostDto updatePost(PostDto postDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(postDto);
                UpdatePostFunctionalitySagas updatePostFunctionalitySagas = new UpdatePostFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, postService, postDto);
                updatePostFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updatePostFunctionalitySagas.getUpdatedPostDto();
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deletePost(Integer postAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeletePostFunctionalitySagas deletePostFunctionalitySagas = new DeletePostFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, postService, postAggregateId);
                deletePostFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<PostDto> getAllPosts() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllPostsFunctionalitySagas getAllPostsFunctionalitySagas = new GetAllPostsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, postService);
                getAllPostsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllPostsFunctionalitySagas.getPosts();
            default: throw new EventdrivenException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(PostDto postDto) {
        if (postDto.getTitle() == null) {
            throw new EventdrivenException(POST_MISSING_TITLE);
        }
        if (postDto.getContent() == null) {
            throw new EventdrivenException(POST_MISSING_CONTENT);
        }
}

    private void checkInput(CreatePostRequestDto createRequest) {
        if (createRequest.getTitle() == null) {
            throw new EventdrivenException(POST_MISSING_TITLE);
        }
        if (createRequest.getContent() == null) {
            throw new EventdrivenException(POST_MISSING_CONTENT);
        }
}
}