package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.helloworld.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.helloworld.command.task.*;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllTasksFunctionalitySagas extends WorkflowFunctionality {
    private List<TaskDto> tasks;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllTasksFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllTasksStep = new SagaStep("getAllTasksStep", () -> {
            GetAllTasksCommand cmd = new GetAllTasksCommand(unitOfWork, ServiceMapping.TASK.getServiceName());
            List<TaskDto> tasks = (List<TaskDto>) commandGateway.send(cmd);
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
