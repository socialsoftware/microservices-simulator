package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.CourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class ExecutionEventProcessing {
    @Autowired
    private ExecutionService executionService;

    @Autowired
    private ExecutionFactory executionFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public ExecutionEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processCourseDeletedEvent(Integer aggregateId, CourseDeletedEvent courseDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
        newExecution.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
        newExecution.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUserUpdatedEvent(Integer aggregateId, UserUpdatedEvent userUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        executionService.handleUserUpdatedEvent(aggregateId, userUpdatedEvent.getPublisherAggregateId(), userUpdatedEvent.getPublisherAggregateVersion(), userUpdatedEvent.getName(), userUpdatedEvent.getUsername(), userUpdatedEvent.getActive(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}