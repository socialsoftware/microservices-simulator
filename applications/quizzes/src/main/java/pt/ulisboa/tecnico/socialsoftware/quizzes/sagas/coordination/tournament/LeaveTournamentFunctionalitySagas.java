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
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.LeaveTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class LeaveTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto oldTournament;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public LeaveTournamentFunctionalitySagas(TournamentService tournamentService, SagaUnitOfWorkService unitOfWorkService,
                                             TournamentFactory tournamentFactory,
                                             Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentFactory, tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(TournamentFactory tournamentFactory, Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getOldTournamentStep = new SagaSyncStep("getOldTournamentStep", () -> {
//            TournamentDto oldTournament = (TournamentDto) tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
//            unitOfWorkService.registerSagaState(tournamentAggregateId, TournamentSagaState.READ_TOURNAMENT, unitOfWork);
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            getTournamentByIdCommand.setSemanticLock(TournamentSagaState.READ_TOURNAMENT);
            TournamentDto oldTournament = (TournamentDto) commandGateway.send(getTournamentByIdCommand);
            this.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(tournamentAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);
    
        SagaSyncStep leaveTournamentStep = new SagaSyncStep("leaveTournamentStep", () -> {
//            tournamentService.leaveTournament(tournamentAggregateId, userAggregateId, unitOfWork);
            LeaveTournamentCommand leaveTournamentCommand = new LeaveTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, userAggregateId);
            commandGateway.send(leaveTournamentCommand);
        }, new ArrayList<>(Arrays.asList(getOldTournamentStep)));
    
        workflow.addStep(getOldTournamentStep);
        workflow.addStep(leaveTournamentStep);
    }

    public TournamentDto getOldTournament() {
        return oldTournament;
    }

    public void setOldTournament(TournamentDto oldTournament) {
        this.oldTournament = oldTournament;
    }
}