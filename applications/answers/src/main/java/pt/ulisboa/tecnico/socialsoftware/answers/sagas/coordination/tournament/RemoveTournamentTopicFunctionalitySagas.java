package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveTournamentTopicFunctionalitySagas extends WorkflowFunctionality {
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public RemoveTournamentTopicFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, Integer tournamentId, Integer topicAggregateId) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentId, topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep removeTopicStep = new SagaSyncStep("removeTopicStep", () -> {
            tournamentService.removeTournamentTopic(tournamentId, topicAggregateId, unitOfWork);
        });

        workflow.addStep(removeTopicStep);
    }
}
