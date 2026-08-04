package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;

import java.util.ArrayList;
import java.util.Arrays;

public class AddParticipantFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddParticipantFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             Integer tournamentId, Integer executionId, Integer userId,
                                             SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, executionId, userId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer executionId, Integer userId,
                               SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand cmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userId);
            this.userDto = (UserDto) commandGateway.send(cmd);
        });

        SagaStep getStudentStep = new SagaStep("getStudentStep", () -> {
            // P4a: throws COURSE_EXECUTION_STUDENT_NOT_FOUND if participant not enrolled
            GetStudentByExecutionIdAndUserIdCommand cmd = new GetStudentByExecutionIdAndUserIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionId, userId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        SagaStep getTournamentStep = new SagaStep("getTournamentStep", () -> {
            GetTournamentByIdCommand getCmd = new GetTournamentByIdCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId);
            SagaCommand sagaCmd = new SagaCommand(getCmd);
            sagaCmd.setSemanticLock(TournamentSagaState.IN_ADD_PARTICIPANT);
            commandGateway.send(sagaCmd);
        }, new ArrayList<>(Arrays.asList(getStudentStep)));

        SagaStep addParticipantStep = new SagaStep("addParticipantStep", () -> {
            AddParticipantCommand cmd = new AddParticipantCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentId,
                    this.userDto.getAggregateId(), this.userDto.getName(),
                    this.userDto.getUsername(), this.userDto.getVersion());
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        this.workflow.addStep(getUserStep);
        this.workflow.addStep(getStudentStep);
        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(addParticipantStep);
    }
}
