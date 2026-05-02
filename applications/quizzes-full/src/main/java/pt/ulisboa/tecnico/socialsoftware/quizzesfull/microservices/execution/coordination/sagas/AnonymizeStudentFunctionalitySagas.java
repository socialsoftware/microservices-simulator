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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.AnonymizeStudentInExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.AnonymizeUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states.ExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class AnonymizeStudentFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AnonymizeStudentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              Integer executionAggregateId, Integer userId,
                                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, userId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand getCmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCmd);
            sagaCommand.setSemanticLock(ExecutionSagaState.IN_ANONYMIZE_STUDENT);
            commandGateway.send(sagaCommand);
        });

        getExecutionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep anonymizeStudentInExecutionStep = new SagaStep("anonymizeStudentInExecutionStep", () -> {
            AnonymizeStudentInExecutionCommand anonymizeCmd = new AnonymizeStudentInExecutionCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId, userId);
            commandGateway.send(anonymizeCmd);
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        SagaStep anonymizeUserStep = new SagaStep("anonymizeUserStep", () -> {
            AnonymizeUserCommand anonymizeUserCmd = new AnonymizeUserCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userId);
            commandGateway.send(anonymizeUserCmd);
        }, new ArrayList<>(Arrays.asList(anonymizeStudentInExecutionStep)));

        workflow.addStep(getExecutionStep);
        workflow.addStep(anonymizeStudentInExecutionStep);
        workflow.addStep(anonymizeUserStep);
    }
}
