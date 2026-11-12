package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.user.GetUserByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.util.ArrayList;
import java.util.Arrays;

public class AddParticipantFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private UserDto userDto;
    private ExecutionDto executionDto;

    public AddParticipantFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            Integer tournamentAggregateId, Integer userAggregateId,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, tournamentAggregateId, userAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId,
                              Integer userAggregateId, SagaUnitOfWork unitOfWork,
                              CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P4a prerequisite: seeds the TournamentParticipant snapshot.
        SagaStep getUserStep = new SagaStep("getUserStep", () -> {
            GetUserByIdCommand cmd = new GetUserByIdCommand(
                    unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            this.userDto = (UserDto) commandGateway.send(cmd);
        });

        SagaStep getTournamentStep = new SagaStep("getTournamentStep", () -> {
            GetTournamentByIdCommand readCmd = new GetTournamentByIdCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(TournamentSagaState.IN_ADD_PARTICIPANT);
            this.tournamentDto = (TournamentDto) commandGateway.send(sagaCommand);
        });

        // Supplies the enrolment list TournamentService checks for PARTICIPANT_COURSE_EXECUTION (P3).
        // AddParticipant is keyed on the tournament alone, so which execution to fetch is only known
        // once the tournament DTO is in hand - this step cannot run alongside the lock step.
        SagaStep getExecutionStep = new SagaStep("getExecutionStep", () -> {
            GetExecutionByIdCommand cmd = new GetExecutionByIdCommand(
                    unitOfWork, ServiceMapping.EXECUTION.getServiceName(),
                    this.tournamentDto.getExecutionAggregateId());
            this.executionDto = (ExecutionDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        SagaStep addParticipantStep = new SagaStep("addParticipantStep", () -> {
            AddParticipantCommand cmd = new AddParticipantCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId,
                    this.userDto, this.executionDto);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getUserStep, getTournamentStep, getExecutionStep)));

        this.workflow.addStep(getUserStep);
        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(getExecutionStep);
        this.workflow.addStep(addParticipantStep);
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }
}
