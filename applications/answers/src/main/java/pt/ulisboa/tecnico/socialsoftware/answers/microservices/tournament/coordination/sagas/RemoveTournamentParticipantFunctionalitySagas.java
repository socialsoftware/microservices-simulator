package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveTournamentParticipantFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveTournamentParticipantFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentId, Integer participantAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, participantAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer participantAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep removeParticipantStep = new SagaStep("removeParticipantStep", () -> {
            RemoveTournamentParticipantCommand cmd = new RemoveTournamentParticipantCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId, participantAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(removeParticipantStep);
    }
}
