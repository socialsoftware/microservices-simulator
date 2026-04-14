package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service.PostService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.events.AuthorUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.events.AuthorDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.Post;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class PostEventProcessing {
    @Autowired
    private PostService postService;

    @Autowired
    private PostFactory postFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public PostEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processAuthorUpdatedEvent(Integer aggregateId, AuthorUpdatedEvent authorUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        postService.handleAuthorUpdatedEvent(aggregateId, authorUpdatedEvent.getPublisherAggregateId(), authorUpdatedEvent.getPublisherAggregateVersion(), authorUpdatedEvent.getName(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processAuthorDeletedEvent(Integer aggregateId, AuthorDeletedEvent authorDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Post oldPost = (Post) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Post newPost = postFactory.createPostFromExisting(oldPost);
        newPost.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newPost, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}