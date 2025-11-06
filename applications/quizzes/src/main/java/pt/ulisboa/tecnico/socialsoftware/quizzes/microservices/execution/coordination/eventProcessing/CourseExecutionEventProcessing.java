package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.RemoveUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.events.publish.DeleteUserEvent;

@Service
public class CourseExecutionEventProcessing {
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private final CommandGateway commandGateway;

    public CourseExecutionEventProcessing(UnitOfWorkService unitOfWorkService, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
    }

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent deleteUserEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        RemoveUserCommand command = new RemoveUserCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), aggregateId, deleteUserEvent.getPublisherAggregateId());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }


}
