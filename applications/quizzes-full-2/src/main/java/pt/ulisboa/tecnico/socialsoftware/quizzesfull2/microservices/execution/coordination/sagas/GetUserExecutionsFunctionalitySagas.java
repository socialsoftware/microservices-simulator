package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetUserExecutionsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;

import java.util.List;

public class GetUserExecutionsFunctionalitySagas extends WorkflowFunctionality {
    private List<ExecutionDto> executions;

    public GetUserExecutionsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer userAggregateId,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              Integer userAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserExecutionsStep = new SagaStep("getUserExecutionsStep", () -> {
            GetUserExecutionsCommand cmd = new GetUserExecutionsCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), userAggregateId);
            this.executions = (List<ExecutionDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getUserExecutionsStep);
    }

    public List<ExecutionDto> getExecutions() {
        return executions;
    }
}
