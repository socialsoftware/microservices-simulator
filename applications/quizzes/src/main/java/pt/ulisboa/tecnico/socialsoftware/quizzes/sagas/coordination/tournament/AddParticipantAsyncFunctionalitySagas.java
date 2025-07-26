package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SagasCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaAsyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaUserDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class AddParticipantAsyncFunctionalitySagas extends WorkflowFunctionality {
    private UserDto userDto;
    private final TournamentService tournamentService;
    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final SagasCommandGateway sagasCommandGateway;

    public AddParticipantAsyncFunctionalitySagas(TournamentService tournamentService,
            CourseExecutionService courseExecutionService,
            SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId,
            Integer courseExecutionAggregateId, Integer userAggregateId,
            SagaUnitOfWork unitOfWork, SagasCommandGateway sagasCommandGateway) {
        this.tournamentService = tournamentService;
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.sagasCommandGateway = sagasCommandGateway;
        this.buildWorkflow(tournamentAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer courseExecutionAggregateId,
            Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaAsyncStep getUserStep = new SagaAsyncStep("getUserStep", () -> {
            return CompletableFuture.runAsync(() -> {
                Logger.getLogger(AddParticipantAsyncFunctionalitySagas.class.getName()).info(
                        "Getting user: " + userAggregateId + " for course execution: " + courseExecutionAggregateId);
                SagaUserDto user = (SagaUserDto) courseExecutionService
                        .getStudentByExecutionIdAndUserId(courseExecutionAggregateId, userAggregateId, unitOfWork);
                this.setUserDto(user);
                // GetStudentByExecutionIdAndUserIdCommand getStudentCommand = new
                // GetStudentByExecutionIdAndUserIdCommand(
                // unitOfWork, "courseExecutionService", courseExecutionAggregateId,
                // userAggregateId);
                // // getStudentCommand.setSemanticLock(UserSagaState.READ_USER);
                // this.userDto = (UserDto) sagasCommandGateway.send(getStudentCommand);
            });
        });

        // getUserStep.registerCompensation(() -> {
        // unitOfWorkService.registerSagaState(userAggregateId,
        // GenericSagaState.NOT_IN_SAGA, unitOfWork);
        // }, unitOfWork);

        SagaAsyncStep addParticipantStep = new SagaAsyncStep("addParticipantStep", () -> {
            return CompletableFuture.supplyAsync(() -> {
                Logger.getLogger(AddParticipantAsyncFunctionalitySagas.class.getName())
                        .info("Adding participant to tournament: " + tournamentAggregateId + " for user: "
                                + this.userDto.getUsername());
                TournamentParticipant participant = new TournamentParticipant(this.userDto);
                // List<SagaAggregate.SagaState> states = new ArrayList<>();
                // states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);

                // AddParticipantCommand addParticipantCommand = new
                // AddParticipantCommand(unitOfWork, "tournamentService",
                // tournamentAggregateId, participant);
                //// addParticipantCommand.setForbiddenStates(states);
                // sagasCommandGateway.send(addParticipantCommand);
                tournamentService.addParticipant(tournamentAggregateId, participant, unitOfWork);
                return null;
            });
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        workflow.addStep(getUserStep);
        workflow.addStep(addParticipantStep);
    }

    public UserDto getUserDto() {
        return userDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }
}