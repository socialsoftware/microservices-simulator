package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CancelTournamentFunctionalitySagas extends WorkflowFunctionality {
    public enum State implements SagaState {
        CANCEL_TOURNAMENT_READ_TOURNAMENT {
            @Override
            public String getStateName() {
                return "CANCEL_TOURNAMENT_READ_TOURNAMENT";
            }
        }
    }
    
    private Tournament oldTournament;

    

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

        SyncStep getOldTournamentStep = new SyncStep("getOldTournamentStep", () -> {
            SagaTournament oldTournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldTournament, State.CANCEL_TOURNAMENT_READ_TOURNAMENT, unitOfWork);
            this.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(this.getOldTournament());
            unitOfWorkService.registerSagaState((SagaTournament) newTournament, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newTournament);
        }, unitOfWork);
    
        SyncStep cancelTournamentStep = new SyncStep("cancelTournamentStep", () -> {
            tournamentService.cancelTournament(tournamentAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldTournamentStep)));
    
        workflow.addStep(getOldTournamentStep);
        workflow.addStep(cancelTournamentStep);
    }

    @Override
    public void handleEvents() {

    }

    

    public Tournament getOldTournament() {
        return oldTournament;
    }

    public void setOldTournament(Tournament oldTournament) {
        this.oldTournament = oldTournament;
    }
}