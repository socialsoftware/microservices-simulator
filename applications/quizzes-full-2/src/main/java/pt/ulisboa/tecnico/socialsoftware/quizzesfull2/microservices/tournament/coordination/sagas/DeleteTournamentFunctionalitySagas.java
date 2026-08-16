package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.DeleteTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto tournamentDto;

    public DeleteTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                              Integer tournamentAggregateId, SagaUnitOfWork unitOfWork,
                                              CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, tournamentAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTournamentStep = new SagaStep("getTournamentStep", () -> {
            GetTournamentByIdCommand readCmd = new GetTournamentByIdCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(TournamentSagaState.IN_DELETE_TOURNAMENT);
            this.tournamentDto = (TournamentDto) commandGateway.send(sagaCommand);
        });

        // The service clears the participant list before remove() so TOURNAMENT_DELETE holds; the
        // clearing is part of the same operation, not a second step.
        SagaStep deleteTournamentStep = new SagaStep("deleteTournamentStep", () -> {
            DeleteTournamentCommand cmd = new DeleteTournamentCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(deleteTournamentStep);
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }
}
