package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class LeaveTournamentFunctionality extends WorkflowFunctionality {
    private Tournament oldTournament;

    private SagaWorkflow workflow;

    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public LeaveTournamentFunctionality(TournamentService tournamentService,SagaUnitOfWorkService unitOfWorkService, 
                                TournamentFactory tournamentFactory,
                                Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentFactory, tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(TournamentFactory tournamentFactory, Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getOldTournamentStep = new SyncStep("getOldTournamentStep", () -> {
            SagaTournament oldTournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldTournament, SagaState.LEAVE_TOURNAMENT_READ_TOURANMENT, unitOfWork);
            this.setOldTournament(oldTournament);
        });
    
        getOldTournamentStep.registerCompensation(() -> {
            Tournament newTournament = tournamentFactory.createTournamentFromExisting(this.getOldTournament());
            unitOfWorkService.registerSagaState((SagaTournament) newTournament, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newTournament);
        }, unitOfWork);
    
        SyncStep leaveTournamentStep = new SyncStep("leaveTournamentStep", () -> {
            tournamentService.leaveTournament(tournamentAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getOldTournamentStep)));
    
        workflow.addStep(getOldTournamentStep);
        workflow.addStep(leaveTournamentStep);
    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    @Override
    public void handleEvents() {

    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
    }

    public Tournament getOldTournament() {
        return oldTournament;
    }

    public void setOldTournament(Tournament oldTournament) {
        this.oldTournament = oldTournament;
    }
}