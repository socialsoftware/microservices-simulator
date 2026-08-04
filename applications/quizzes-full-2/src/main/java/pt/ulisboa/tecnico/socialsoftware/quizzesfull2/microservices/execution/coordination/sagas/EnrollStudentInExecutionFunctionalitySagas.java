package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.EnrollStudentInExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.states.ExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.util.ArrayList;
import java.util.Arrays;

public class EnrollStudentInExecutionFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;
    private ExecutionDto executionDto;

    public EnrollStudentInExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                      Integer executionAggregateId, Integer userAggregateId,
                                                      SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, executionAggregateId, userAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer executionAggregateId,
                              Integer userAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // Data assembly for P3 INACTIVE_USER: the fetched UserDto is what ExecutionService.enrollStudent()
        // validates, and the same DTO seeds the ExecutionStudent snapshot.
        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand cmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            this.userDto = (UserDto) commandGateway.send(cmd);
        });

        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand readCmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(ExecutionSagaState.IN_ENROLL_STUDENT_IN_EXECUTION);
            this.executionDto = (ExecutionDto) commandGateway.send(sagaCommand);
        });

        SagaStep enrollStudentStep = new SagaStep("enrollStudentStep", () -> {
            EnrollStudentInExecutionCommand cmd = new EnrollStudentInExecutionCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId, this.userDto);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getUserStep, getExecutionStep)));

        this.workflow.addStep(getUserStep);
        this.workflow.addStep(getExecutionStep);
        this.workflow.addStep(enrollStudentStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public ExecutionDto getExecutionDto() {
        return executionDto;
    }
}
