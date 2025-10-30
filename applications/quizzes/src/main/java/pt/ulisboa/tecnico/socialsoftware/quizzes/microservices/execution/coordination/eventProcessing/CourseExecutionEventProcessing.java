package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.events.publish.DeleteUserEvent;

@Service
public class CourseExecutionEventProcessing {
    @Autowired
    private CourseExecutionService courseExecutionService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public CourseExecutionEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent deleteUserEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        courseExecutionService.removeUser(aggregateId, deleteUserEvent.getPublisherAggregateId(), deleteUserEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }


}
