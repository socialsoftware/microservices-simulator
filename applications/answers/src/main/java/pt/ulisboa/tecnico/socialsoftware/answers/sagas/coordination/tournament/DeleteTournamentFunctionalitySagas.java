package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteTournamentFunctionalitySagas extends WorkflowFunctionality {
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteTournamentFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, Integer tournamentAggregateId) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteTournamentStep = new SagaSyncStep("deleteTournamentStep", () -> {
            tournamentService.deleteTournament(tournamentAggregateId);
        });

        workflow.addStep(deleteTournamentStep);

    }

}
