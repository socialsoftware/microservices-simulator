package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllExecutionsFunctionalitySagas extends WorkflowFunctionality {
    private List<ExecutionDto> executions;
    private final ExecutionService executionService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllExecutionsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, ExecutionService executionService) {
        this.executionService = executionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllExecutionsStep = new SagaSyncStep("getAllExecutionsStep", () -> {
            List<ExecutionDto> executions = executionService.getAllExecutions();
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
