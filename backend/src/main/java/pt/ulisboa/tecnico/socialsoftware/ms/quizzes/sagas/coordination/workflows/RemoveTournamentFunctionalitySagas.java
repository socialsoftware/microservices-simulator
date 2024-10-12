package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveTournamentFunctionalitySagas extends WorkflowFunctionality {

    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    private String currentStep = "";

    public RemoveTournamentFunctionalitySagas(EventService eventService, TournamentService tournamentService,SagaUnitOfWorkService unitOfWorkService, 
                                TournamentFactory tournamentFactory,
                                Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentFactory, tournamentAggregateId, unitOfWork);
    }

    public void buildWorkflow(TournamentFactory tournamentFactory, Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
    
        SagaSyncStep removeTournamentStep = new SagaSyncStep("removeTournamentStep", () -> {
            tournamentService.removeTournament(tournamentAggregateId, unitOfWork);
        });
    
        workflow.addStep(removeTournamentStep);
    }
}