package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.LeaveTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class LeaveTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto oldTournament;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public LeaveTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                             Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork,
                                             CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getOldTournamentStep = new SagaStep("getOldTournamentStep", () -> {
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            getTournamentByIdCommand.setSemanticLock(TournamentSagaState.READ_TOURNAMENT);
            TournamentDto oldTournament = (TournamentDto) commandGateway.send(getTournamentByIdCommand);
            this.setOldTournament(oldTournament);
        });

        getOldTournamentStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep leaveTournamentStep = new SagaStep("leaveTournamentStep", () -> {
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