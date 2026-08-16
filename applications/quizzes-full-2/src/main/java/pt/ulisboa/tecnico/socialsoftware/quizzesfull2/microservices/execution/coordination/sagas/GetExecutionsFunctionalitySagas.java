package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetExecutionsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;

import java.util.List;

public class GetExecutionsFunctionalitySagas extends WorkflowFunctionality {
    private List<ExecutionDto> executions;

    public GetExecutionsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                           SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, unitOfWork, commandGateway);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getExecutionsStep = new SagaStep("getExecutionsStep", () -> {
            GetExecutionsCommand cmd = new GetExecutionsCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName());
            this.executions = (List<ExecutionDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getExecutionsStep);
    }

    public List<ExecutionDto> getExecutions() {
        return executions;
    }
}
