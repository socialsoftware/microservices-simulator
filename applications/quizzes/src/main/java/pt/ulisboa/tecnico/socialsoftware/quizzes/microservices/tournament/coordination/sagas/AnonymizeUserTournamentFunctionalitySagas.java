package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.AnonymizeUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.List;

public class AnonymizeUserTournamentFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AnonymizeUserTournamentFunctionalitySagas(UnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer executionAggregateId,
                                                     Integer userAggregateId, String name, String username, Integer eventVersion, UnitOfWork unitOfWork,
                                                     CommandGateway commandGateway) {
        this.unitOfWorkService = (SagaUnitOfWorkService) unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, executionAggregateId, userAggregateId, name, username, eventVersion,
                (SagaUnitOfWork) unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId,
            String name, String username, Integer eventVersion, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep anonymizeUserStep = new SagaStep("anonymizeUserStep", () -> {
            AnonymizeUserCommand anonymizeUserCommand = new AnonymizeUserCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, executionAggregateId, userAggregateId, name, username, eventVersion);
            anonymizeUserCommand.setForbiddenStates(new ArrayList<>(List.of(TournamentSagaState.IN_UPDATE_TOURNAMENT)));
            commandGateway.send(anonymizeUserCommand);
        });

        this.workflow.addStep(anonymizeUserStep);
    }

}