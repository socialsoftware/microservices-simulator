package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.CancelTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class CancelTournamentFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CancelTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                               Integer tournamentId,
                                               SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTournamentStep = new SagaStep("getTournamentStep", () -> {
            GetTournamentByIdCommand getCmd = new GetTournamentByIdCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId);
            SagaCommand sagaCmd = new SagaCommand(getCmd);
            sagaCmd.setSemanticLock(TournamentSagaState.IN_CANCEL_TOURNAMENT);
            commandGateway.send(sagaCmd);
        });

        getTournamentStep.registerCompensation(() -> {
            Command releaseCmd = new Command(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId);
            SagaCommand release = new SagaCommand(releaseCmd);
            release.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(release);
        }, unitOfWork);

        SagaStep cancelTournamentStep = new SagaStep("cancelTournamentStep", () -> {
            CancelTournamentCommand cmd = new CancelTournamentCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(cancelTournamentStep);
    }
}
