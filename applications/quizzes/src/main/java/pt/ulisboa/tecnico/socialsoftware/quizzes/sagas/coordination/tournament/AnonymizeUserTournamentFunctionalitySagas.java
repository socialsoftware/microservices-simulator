package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.AnonymizeUserCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.List;

public class AnonymizeUserTournamentFunctionalitySagas extends WorkflowFunctionality {

    private TournamentService tournamentService;
    private SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public AnonymizeUserTournamentFunctionalitySagas(TournamentService tournamentService,
            UnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer executionAggregateId,
            Integer userAggregateId, String name, String username, Integer eventVersion, UnitOfWork unitOfWork,
            CommandGateway CommandGateway) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = (SagaUnitOfWorkService) unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(tournamentAggregateId, executionAggregateId, userAggregateId, name, username, eventVersion,
                (SagaUnitOfWork) unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId,
            String name, String username, Integer eventVersion, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep anonymizeUserStep = new SagaSyncStep("anonymizeUserStep", () -> {
            // List<SagaAggregate.SagaState> states = new ArrayList<>();
            // states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            // unitOfWorkService.verifySagaState(tournamentAggregateId, states);
            // tournamentService.anonymizeUser(tournamentAggregateId, executionAggregateId,
            // userAggregateId, name, username, eventVersion, unitOfWork);
            AnonymizeUserCommand anonymizeUserCommand = new AnonymizeUserCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, executionAggregateId,
                    userAggregateId, name, username, eventVersion);
            anonymizeUserCommand.setForbiddenStates(new ArrayList<>(List.of(TournamentSagaState.IN_UPDATE_TOURNAMENT)));
            CommandGateway.send(anonymizeUserCommand);
        });

        this.workflow.addStep(anonymizeUserStep);
    }

}