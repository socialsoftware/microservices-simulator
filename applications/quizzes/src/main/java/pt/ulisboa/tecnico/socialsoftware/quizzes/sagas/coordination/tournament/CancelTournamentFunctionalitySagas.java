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
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.CancelTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class CancelTournamentFunctionalitySagas extends WorkflowFunctionality {
    
    private TournamentDto tournament;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CancelTournamentFunctionalitySagas(TournamentService tournamentService, SagaUnitOfWorkService unitOfWorkService,
                                              TournamentFactory tournamentFactory,
                                              Integer tournamentAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentFactory, tournamentAggregateId, unitOfWork);
    }

    public void buildWorkflow(TournamentFactory tournamentFactory, Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
//            SagaTournamentDto tournament = (SagaTournamentDto) tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
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
    
        SagaSyncStep cancelTournamentStep = new SagaSyncStep("cancelTournamentStep", () -> {
//            tournamentService.cancelTournament(tournamentAggregateId, unitOfWork);
            CancelTournamentCommand cancelTournamentCommand = new CancelTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            commandGateway.send(cancelTournamentCommand);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));
    
        workflow.addStep(getTournamentStep);
        workflow.addStep(cancelTournamentStep);
    }
    

    public TournamentDto getTournament() {
        return tournament;
    }

    public void setTournament(TournamentDto Tournament) {
        this.tournament = Tournament;
    }
}