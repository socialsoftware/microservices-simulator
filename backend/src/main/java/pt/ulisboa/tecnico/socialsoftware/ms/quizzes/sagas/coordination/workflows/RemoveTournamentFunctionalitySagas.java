package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveTournamentFunctionalitySagas extends WorkflowFunctionality {

    private Tournament tournament;
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

        SyncStep gettournamentStep = new SyncStep("getTournamentStep", () -> {
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournament, TournamentSagaState.REMOVE_TOURNAMENT_READ_TOURNAMENT, unitOfWork);
            this.setTournament(tournament);
            this.currentStep = "getTournamentStep";
        });
    
        gettournamentStep.registerCompensation(() -> {
            if (this.getTournament() != null) {
                Tournament newTournament = tournamentFactory.createTournamentFromExisting(this.getTournament());
                unitOfWorkService.registerSagaState((SagaTournament) newTournament, GenericSagaState.NOT_IN_SAGA, unitOfWork);
                unitOfWork.registerChanged(newTournament);
            }
        }, unitOfWork);
    
        SyncStep removeTournamentStep = new SyncStep("removeTournamentStep", () -> {
            tournamentService.removeTournament(tournamentAggregateId, unitOfWork);
            this.currentStep = "removeTournamentStep";
        }, new ArrayList<>(Arrays.asList(gettournamentStep)));
    
        workflow.addStep(gettournamentStep);
        workflow.addStep(removeTournamentStep);
    }

    @Override
    public void handleEvents() {
        if (currentStep.equals("getTournamentStep") || currentStep.equals("removeTournamentStep")) {
        
            Set<EventSubscription> eventSubscriptions = tournament.getEventSubscriptions();

            for (EventSubscription eventSubscription: eventSubscriptions) {
                // TODO missing events
            } 
        } 
    }

    
    
    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }
}