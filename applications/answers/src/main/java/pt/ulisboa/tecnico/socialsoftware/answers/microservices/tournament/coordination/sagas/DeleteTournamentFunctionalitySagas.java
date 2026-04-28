package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

public class DeleteTournamentFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteTournamentStep = new SagaStep("deleteTournamentStep", () -> {
            unitOfWorkService.verifySagaState(tournamentAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(TournamentSagaState.READ_TOURNAMENT, TournamentSagaState.UPDATE_TOURNAMENT, TournamentSagaState.DELETE_TOURNAMENT)));
            unitOfWorkService.registerSagaState(tournamentAggregateId, TournamentSagaState.DELETE_TOURNAMENT, unitOfWork);
            DeleteTournamentCommand cmd = new DeleteTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteTournamentStep);
    }
}
