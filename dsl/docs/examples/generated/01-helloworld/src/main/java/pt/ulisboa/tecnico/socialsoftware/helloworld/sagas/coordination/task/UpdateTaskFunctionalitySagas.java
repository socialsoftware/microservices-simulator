package pt.ulisboa.tecnico.socialsoftware.helloworld.sagas.coordination.task;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.service.TaskService;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTaskFunctionalitySagas extends WorkflowFunctionality {
    private TaskDto updatedTaskDto;
    private final TaskService taskService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateTaskFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TaskService taskService, TaskDto taskDto) {
        this.taskService = taskService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(taskDto, unitOfWork);
    }

    public void buildWorkflow(TaskDto taskDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateTaskStep = new SagaSyncStep("updateTaskStep", () -> {
            TaskDto updatedTaskDto = taskService.updateTask(taskDto, unitOfWork);
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
