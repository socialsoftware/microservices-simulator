package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.helloworld.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.helloworld.command.task.*;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.states.TaskSagaState;

public class UpdateTaskFunctionalitySagas extends WorkflowFunctionality {
    private TaskDto updatedTaskDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateTaskFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, TaskDto taskDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(taskDto, unitOfWork);
    }

    public void buildWorkflow(TaskDto taskDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateTaskStep = new SagaStep("updateTaskStep", () -> {
            unitOfWorkService.verifySagaState(taskDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(TaskSagaState.READ_TASK, TaskSagaState.UPDATE_TASK, TaskSagaState.DELETE_TASK)));
            unitOfWorkService.registerSagaState(taskDto.getAggregateId(), TaskSagaState.UPDATE_TASK, unitOfWork);
            UpdateTaskCommand cmd = new UpdateTaskCommand(unitOfWork, ServiceMapping.TASK.getServiceName(), taskDto);
            TaskDto updatedTaskDto = (TaskDto) commandGateway.send(cmd);
            setUpdatedTaskDto(updatedTaskDto);
        });

        workflow.addStep(updateTaskStep);
    }
    public TaskDto getUpdatedTaskDto() {
        return updatedTaskDto;
    }

    public void setUpdatedTaskDto(TaskDto updatedTaskDto) {
        this.updatedTaskDto = updatedTaskDto;
    }
}
