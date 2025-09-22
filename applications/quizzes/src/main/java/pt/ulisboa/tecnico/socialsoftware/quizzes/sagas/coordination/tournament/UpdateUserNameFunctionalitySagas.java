package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateUserNameCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateUserNameFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournament;
    private UserDto participant;
    private String name;
    private final CommandGateway CommandGateway;
    private Integer eventVersion;
    private Integer executionAggregateId;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateUserNameFunctionalitySagas(TournamentService tournamentService,
            SagaUnitOfWorkService unitOfWorkService, Integer eventVersion, Integer tournamentAggregateId,
            Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork, String name,
            CommandGateway CommandGateway) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.eventVersion = eventVersion;
        this.executionAggregateId = executionAggregateId;
        this.name = name;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
            // TournamentDto tournamentDTO = (TournamentDto)
            // tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            // unitOfWorkService.registerSagaState(tournamentAggregateId,
            // TournamentSagaState.READ_TOURNAMENT, unitOfWork);
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            getTournamentByIdCommand.setSemanticLock(TournamentSagaState.READ_TOURNAMENT);
            TournamentDto tournamentDTO = (TournamentDto) CommandGateway.send(getTournamentByIdCommand);
            this.setTournament(tournamentDTO);

        });

        getTournamentStep.registerCompensation(() -> {
            // unitOfWorkService.registerSagaState(tournamentAggregateId,
            // GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            CommandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep getParticipantStep = new SagaSyncStep("getParticipantStep", () -> {
            UserDto participant = this.tournament.getParticipants().stream()
                    .filter(p -> p.getAggregateId().equals(userAggregateId)).findFirst().orElse(null);
            this.setParticipant(participant);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        getParticipantStep.registerCompensation(() -> {
            // unitOfWorkService.registerSagaState(userAggregateId,
            // GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.USER.getServiceName(), userAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            CommandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep updateParticipantNameStep = new SagaSyncStep("updateParticipantNameStep", () -> {
            // tournamentService.updateUserName(tournamentAggregateId, executionAggregateId,
            // eventVersion, userAggregateId, name, unitOfWork);
            UpdateUserNameCommand updateUserNameCommand = new UpdateUserNameCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, executionAggregateId,
                    eventVersion, userAggregateId, name);
            CommandGateway.send(updateUserNameCommand);
            this.setParticipant(getParticipant());
            this.setTournament(getTournament());
        }, new ArrayList<>(Arrays.asList(getTournamentStep, getParticipantStep)));

        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(getParticipantStep);
        this.workflow.addStep(updateParticipantNameStep);
    }

    public void setTournament(TournamentDto tournament) {
        this.tournament = tournament;
    }

    public TournamentDto getTournament() {
        return tournament;
    }

    public void setParticipant(UserDto participant) {
        this.participant = participant;
    }

    public UserDto getParticipant() {
        return this.participant;
    }
}