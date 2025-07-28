package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SagasCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddParticipantFunctionalitySagas extends WorkflowFunctionality {
    private TournamentService tournamentService;
    private CourseExecutionService courseExecutionService;
    private SagaUnitOfWorkService unitOfWorkService;
    private UserDto userDto;
    private SagasCommandGateway commandGateway;

    public AddParticipantFunctionalitySagas(TournamentService tournamentService,
            CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService,
            Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId,
            SagaUnitOfWork unitOfWork, SagasCommandGateway commandGateway) {
        this.tournamentService = tournamentService;
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId,
            SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            GetStudentByExecutionIdAndUserIdCommand getStudentCommand = new GetStudentByExecutionIdAndUserIdCommand(
                    unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId,
                    userAggregateId);
            this.userDto = (UserDto) commandGateway.send(getStudentCommand);
        });

        SagaSyncStep addParticipantStep = new SagaSyncStep("addParticipantStep", () -> {
            TournamentParticipant participant = new TournamentParticipant(this.userDto);
            List<SagaAggregate.SagaState> states = new ArrayList<>();
            states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            // unitOfWorkService.verifySagaState(tournamentAggregateId, states);
            // tournamentService.addParticipant(tournamentAggregateId, participant,
            // unitOfWork);
            AddParticipantCommand addParticipantCommand = new AddParticipantCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentAggregateId, participant);
            addParticipantCommand.setForbiddenStates(states);
            commandGateway.send(addParticipantCommand);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        this.workflow.addStep(getUserStep);
        this.workflow.addStep(addParticipantStep);
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}