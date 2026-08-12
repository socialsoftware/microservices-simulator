package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.execution.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllExecutionsFunctionalitySagas extends WorkflowFunctionality {
    private List<ExecutionDto> executions;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllExecutionsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllExecutionsStep = new SagaStep("getAllExecutionsStep", () -> {
            GetAllExecutionsCommand cmd = new GetAllExecutionsCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName());
            List<ExecutionDto> executions = (List<ExecutionDto>) commandGateway.send(cmd);
            setExecutions(executions);
        });

        workflow.addStep(getAllExecutionsStep);
    }
    public List<ExecutionDto> getExecutions() {
        return executions;
    }

    public void setExecutions(List<ExecutionDto> executions) {
        this.executions = executions;
    }
}
