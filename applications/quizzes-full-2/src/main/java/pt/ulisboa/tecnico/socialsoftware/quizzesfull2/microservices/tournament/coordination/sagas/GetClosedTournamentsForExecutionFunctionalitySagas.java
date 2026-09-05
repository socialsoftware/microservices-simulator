package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetClosedTournamentsForExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;

import java.util.List;

public class GetClosedTournamentsForExecutionFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentDto> tournaments;

    public GetClosedTournamentsForExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                              Integer executionAggregateId,
                                                              SagaUnitOfWork unitOfWork,
                                                              CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, executionAggregateId, unitOfWork, commandGateway);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              Integer executionAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getClosedTournamentsForExecutionStep = new SagaStep(
                "getClosedTournamentsForExecutionStep", () -> {
            GetClosedTournamentsForExecutionCommand cmd = new GetClosedTournamentsForExecutionCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), executionAggregateId);
            this.tournaments = (List<TournamentDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getClosedTournamentsForExecutionStep);
    }

    public List<TournamentDto> getTournaments() {
        return tournaments;
    }
}
