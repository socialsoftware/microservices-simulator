package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament;


import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

import java.util.ArrayList;
import java.util.List;

public class AnonymizeUserTournamentFunctionalitySagas extends WorkflowFunctionality {

    private TournamentService tournamentService;
    private SagaUnitOfWorkService unitOfWorkService;


    public AnonymizeUserTournamentFunctionalitySagas(TournamentService tournamentService, UnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId, String name, String username, Integer eventVersion, UnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = (SagaUnitOfWorkService) unitOfWorkService;
        this.buildWorkflow(tournamentAggregateId, executionAggregateId, userAggregateId, name, username, eventVersion, (SagaUnitOfWork) unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId, String name, String username, Integer eventVersion, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep anonymizeUserStep = new SagaSyncStep("anonymizeUserStep", () -> {
            List<SagaAggregate.SagaState> states = new ArrayList<>();
            states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            unitOfWorkService.verifySagaState(tournamentAggregateId, states);
            tournamentService.anonymizeUser(tournamentAggregateId, executionAggregateId, userAggregateId, name, username, eventVersion, unitOfWork);
        });

        this.workflow.addStep(anonymizeUserStep);
    }

}