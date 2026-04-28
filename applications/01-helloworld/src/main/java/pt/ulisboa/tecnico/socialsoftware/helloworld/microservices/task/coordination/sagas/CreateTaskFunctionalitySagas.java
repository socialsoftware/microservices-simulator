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
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.webapi.requestDtos.CreateTaskRequestDto;

public class CreateTaskFunctionalitySagas extends WorkflowFunctionality {
    private TaskDto createdTaskDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateTaskFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateTaskRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTaskRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createTaskStep = new SagaStep("createTaskStep", () -> {
            CreateTaskCommand cmd = new CreateTaskCommand(unitOfWork, ServiceMapping.TASK.getServiceName(), createRequest);
            TaskDto createdTaskDto = (TaskDto) commandGateway.send(cmd);
            setCreatedTaskDto(createdTaskDto);
        });

        workflow.addStep(createTaskStep);
    }
    public TaskDto getCreatedTaskDto() {
        return createdTaskDto;
    }

    public void setCreatedTaskDto(TaskDto createdTaskDto) {
        this.createdTaskDto = createdTaskDto;
    }
}
