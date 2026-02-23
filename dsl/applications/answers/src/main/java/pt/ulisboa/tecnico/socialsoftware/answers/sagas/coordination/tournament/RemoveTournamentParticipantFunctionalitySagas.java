package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveTournamentParticipantFunctionalitySagas extends WorkflowFunctionality {
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public RemoveTournamentParticipantFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, Integer tournamentId, Integer participantAggregateId) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentId, participantAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer participantAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep removeParticipantStep = new SagaSyncStep("removeParticipantStep", () -> {
            tournamentService.removeTournamentParticipant(tournamentId, participantAggregateId, unitOfWork);
        });

        workflow.addStep(removeParticipantStep);
    }
}
