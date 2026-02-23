package pt.ulisboa.tecnico.socialsoftware.helloworld.sagas.coordination.task;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.service.TaskService;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.helloworld.coordination.webapi.requestDtos.CreateTaskRequestDto;

public class CreateTaskFunctionalitySagas extends WorkflowFunctionality {
    private TaskDto createdTaskDto;
    private final TaskService taskService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateTaskFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TaskService taskService, CreateTaskRequestDto createRequest) {
        this.taskService = taskService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTaskRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createTaskStep = new SagaSyncStep("createTaskStep", () -> {
            TaskDto createdTaskDto = taskService.createTask(createRequest, unitOfWork);
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
