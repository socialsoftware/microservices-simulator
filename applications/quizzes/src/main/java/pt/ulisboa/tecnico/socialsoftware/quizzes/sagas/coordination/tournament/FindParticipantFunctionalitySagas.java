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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaTournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class FindParticipantFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournament;
    private UserDto participant;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public FindParticipantFunctionalitySagas(TournamentService tournamentService, SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
//            TournamentDto tournament = (TournamentDto) tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
//            unitOfWorkService.registerSagaState(tournamentAggregateId, TournamentSagaState.READ_TOURNAMENT, unitOfWork);
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            getTournamentByIdCommand.setSemanticLock(TournamentSagaState.READ_TOURNAMENT);
            TournamentDto tournament = (TournamentDto) commandGateway.send(getTournamentByIdCommand);
            this.setTournament(tournament);
        });

        getTournamentStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(tournamentAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep getParticipantStep = new SagaSyncStep("getParticipantStep", () -> {
            UserDto participant = getTournament().getParticipants().stream().filter(p -> p.getAggregateId().equals(userAggregateId)).findFirst().orElse(null);
            this.setParticipant(participant);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(getParticipantStep);
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