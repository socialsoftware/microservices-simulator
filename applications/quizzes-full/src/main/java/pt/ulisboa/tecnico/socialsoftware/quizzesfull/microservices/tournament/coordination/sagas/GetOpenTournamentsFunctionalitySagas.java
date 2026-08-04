package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.tournament.GetOpenTournamentsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentDto;

import java.util.List;

public class GetOpenTournamentsFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentDto> tournaments;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetOpenTournamentsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer executionAggregateId,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getOpenTournamentsStep = new SagaStep("getOpenTournamentsStep", () -> {
            GetOpenTournamentsCommand cmd = new GetOpenTournamentsCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), executionAggregateId);
            this.tournaments = (List<TournamentDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getOpenTournamentsStep);
    }

    public List<TournamentDto> getTournaments() {
        return tournaments;
    }
}
