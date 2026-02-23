package pt.ulisboa.tecnico.socialsoftware.helloworld.sagas.coordination.task;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.service.TaskService;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllTasksFunctionalitySagas extends WorkflowFunctionality {
    private List<TaskDto> tasks;
    private final TaskService taskService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllTasksFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TaskService taskService) {
        this.taskService = taskService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllTasksStep = new SagaSyncStep("getAllTasksStep", () -> {
            List<TaskDto> tasks = taskService.getAllTasks(unitOfWork);
            setTasks(tasks);
        });

        workflow.addStep(getAllTasksStep);
    }
    public List<TaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
    }
}
