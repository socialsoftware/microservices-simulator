package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.UpdateStudentNameInExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.UpdateUserNameCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states.ExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateStudentNameFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateStudentNameFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer executionAggregateId, Integer userId, String name,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, userId, name, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userId, String name, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand getCmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCmd);
            sagaCommand.setSemanticLock(ExecutionSagaState.IN_UPDATE_STUDENT_NAME);
            commandGateway.send(sagaCommand);
        });

        getExecutionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep updateStudentNameInExecutionStep = new SagaStep("updateStudentNameInExecutionStep", () -> {
            UpdateStudentNameInExecutionCommand updateCmd = new UpdateStudentNameInExecutionCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId, userId, name);
            commandGateway.send(updateCmd);
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        SagaStep updateUserNameStep = new SagaStep("updateUserNameStep", () -> {
            UpdateUserNameCommand updateUserCmd = new UpdateUserNameCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userId, name);
            commandGateway.send(updateUserCmd);
        }, new ArrayList<>(Arrays.asList(updateStudentNameInExecutionStep)));

        workflow.addStep(getExecutionStep);
        workflow.addStep(updateStudentNameInExecutionStep);
        workflow.addStep(updateUserNameStep);
    }
}
