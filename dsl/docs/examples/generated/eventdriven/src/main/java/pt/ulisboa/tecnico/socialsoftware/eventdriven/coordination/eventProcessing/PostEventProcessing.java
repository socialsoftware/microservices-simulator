package pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service.PostService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.events.publish.AuthorDeletedEvent;

@Service
public class PostEventProcessing {
    @Autowired
    private PostService postService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public PostEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processAuthorDeletedEvent(Integer aggregateId, AuthorDeletedEvent authorDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}