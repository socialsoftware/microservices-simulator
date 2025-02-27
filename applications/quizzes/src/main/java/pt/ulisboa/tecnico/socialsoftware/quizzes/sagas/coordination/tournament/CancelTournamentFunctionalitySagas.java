package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.tournament;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CancelTournamentFunctionalitySagas extends WorkflowFunctionality {
    
    private SagaTournamentDto tournament;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public CancelTournamentFunctionalitySagas(TournamentService tournamentService,SagaUnitOfWorkService unitOfWorkService, 
                                TournamentFactory tournamentFactory,
                                Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentFactory, tournamentAggregateId, unitOfWork);
    }

    public void buildWorkflow(TournamentFactory tournamentFactory, Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
            SagaTournamentDto tournament = (SagaTournamentDto) tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournamentAggregateId, TournamentSagaState.READ_TOURNAMENT, unitOfWork);
            this.setTournament(tournament);
        });
    
        getTournamentStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(tournamentAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep cancelTournamentStep = new SagaSyncStep("cancelTournamentStep", () -> {
            tournamentService.cancelTournament(tournamentAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));
    
        workflow.addStep(getTournamentStep);
        workflow.addStep(cancelTournamentStep);
    }
    

    public SagaTournamentDto getTournament() {
        return tournament;
    }

    public void setTournament(SagaTournamentDto Tournament) {
        this.tournament = Tournament;
    }
}