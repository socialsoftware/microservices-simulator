package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.helloworld.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.helloworld.command.task.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.states.TaskSagaState;

public class DeleteTaskFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteTaskFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer taskAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(taskAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer taskAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteTaskStep = new SagaStep("deleteTaskStep", () -> {
            unitOfWorkService.verifySagaState(taskAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(TaskSagaState.READ_TASK, TaskSagaState.UPDATE_TASK, TaskSagaState.DELETE_TASK)));
            unitOfWorkService.registerSagaState(taskAggregateId, TaskSagaState.DELETE_TASK, unitOfWork);
            DeleteTaskCommand cmd = new DeleteTaskCommand(unitOfWork, ServiceMapping.TASK.getServiceName(), taskAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteTaskStep);
    }
}
