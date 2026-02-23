package pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;

@Service
public class ExecutionEventProcessing {
    @Autowired
    private ExecutionService executionService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public ExecutionEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processCourseDeletedEvent(Integer aggregateId, CourseDeletedEvent courseDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}