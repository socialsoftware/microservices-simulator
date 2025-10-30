package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AddParticipantAsyncFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private CompletableFuture<UserDto> userDto;
    private final CommandGateway commandGateway;

    public AddParticipantAsyncFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                 Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId,
                                                 SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId,
            SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            GetStudentByExecutionIdAndUserIdCommand getStudentCommand = new GetStudentByExecutionIdAndUserIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId, userAggregateId);
            this.userDto = commandGateway.sendAsync(getStudentCommand).thenApply(dto -> (UserDto) dto);
        });

        SagaSyncStep addParticipantStep = new SagaSyncStep("addParticipantStep", () -> {
            List<SagaAggregate.SagaState> states = new ArrayList<>();
            states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);

            AddParticipantCommand addParticipantCommand;
            try {
                addParticipantCommand = new AddParticipantCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, this.userDto.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            addParticipantCommand.setForbiddenStates(states);
            commandGateway.send(addParticipantCommand);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        this.workflow.addStep(getUserStep);
        this.workflow.addStep(addParticipantStep);
    }

}