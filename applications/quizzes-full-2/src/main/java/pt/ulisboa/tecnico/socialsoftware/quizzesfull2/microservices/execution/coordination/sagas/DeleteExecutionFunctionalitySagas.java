package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.DeleteExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.states.ExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto executionDto;

    public DeleteExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer executionAggregateId,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, executionAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer executionAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand readCmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(ExecutionSagaState.IN_DELETE_EXECUTION);
            this.executionDto = (ExecutionDto) commandGateway.send(sagaCommand);
        });

        SagaStep deleteExecutionStep = new SagaStep("deleteExecutionStep", () -> {
            DeleteExecutionCommand cmd = new DeleteExecutionCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        this.workflow.addStep(getExecutionStep);
        this.workflow.addStep(deleteExecutionStep);
    }

    public ExecutionDto getExecutionDto() {
        return executionDto;
    }
}
