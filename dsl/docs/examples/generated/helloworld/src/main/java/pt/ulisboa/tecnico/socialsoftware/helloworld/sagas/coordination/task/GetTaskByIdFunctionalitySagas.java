package pt.ulisboa.tecnico.socialsoftware.helloworld.sagas.coordination.task;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.service.TaskService;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetTaskByIdFunctionalitySagas extends WorkflowFunctionality {
    private TaskDto taskDto;
    private final TaskService taskService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetTaskByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TaskService taskService, Integer taskAggregateId) {
        this.taskService = taskService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(taskAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer taskAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTaskStep = new SagaSyncStep("getTaskStep", () -> {
            TaskDto taskDto = taskService.getTaskById(taskAggregateId, unitOfWork);
            setTaskDto(taskDto);
        });

        workflow.addStep(getTaskStep);
    }
    public TaskDto getTaskDto() {
        return taskDto;
    }

    public void setTaskDto(TaskDto taskDto) {
        this.taskDto = taskDto;
    }
}
