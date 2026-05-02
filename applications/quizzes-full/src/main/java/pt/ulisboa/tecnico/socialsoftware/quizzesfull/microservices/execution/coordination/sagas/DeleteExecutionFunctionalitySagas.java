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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.course.DecrementExecutionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.DeleteExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states.ExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteExecutionFunctionalitySagas extends WorkflowFunctionality {
    private ExecutionDto executionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             Integer executionAggregateId,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand getCmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(getCmd);
            sagaCommand.setSemanticLock(ExecutionSagaState.IN_DELETE_EXECUTION);
            this.executionDto = (ExecutionDto) commandGateway.send(sagaCommand);
        });

        getExecutionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(command);
            sagaCommand.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(sagaCommand);
        }, unitOfWork);

        SagaStep deleteExecutionStep = new SagaStep("deleteExecutionStep", () -> {
            DeleteExecutionCommand deleteCmd = new DeleteExecutionCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            commandGateway.send(deleteCmd);
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        SagaStep decrementCourseExecutionCountStep = new SagaStep("decrementCourseExecutionCountStep", () -> {
            DecrementExecutionCountCommand decrementCmd = new DecrementExecutionCountCommand(
                    unitOfWork, ServiceMapping.COURSE.getServiceName(), this.executionDto.getCourseId());
            commandGateway.send(decrementCmd);
        }, new ArrayList<>(Arrays.asList(deleteExecutionStep)));

        workflow.addStep(getExecutionStep);
        workflow.addStep(deleteExecutionStep);
        workflow.addStep(decrementCourseExecutionCountStep);
    }

    public ExecutionDto getExecutionDto() { return executionDto; }
}
