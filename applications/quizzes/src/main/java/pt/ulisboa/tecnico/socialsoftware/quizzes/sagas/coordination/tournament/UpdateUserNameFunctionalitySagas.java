package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaTournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TournamentSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateUserNameFunctionalitySagas extends WorkflowFunctionality {
    private SagaTournamentDto tournament;
    private UserDto participant;    
    private String name;
    private Integer eventVersion;
    private Integer executionAggregateId;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateUserNameFunctionalitySagas(TournamentService tournamentService, SagaUnitOfWorkService unitOfWorkService,Integer eventVersion ,Integer tournamentAggregateId ,Integer executionAggregateId , Integer userAggregateId, SagaUnitOfWork unitOfWork, String name) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.eventVersion = eventVersion;
        this.executionAggregateId = executionAggregateId;
        this.name = name;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
            SagaTournamentDto tournamentDTO = (SagaTournamentDto) tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournamentAggregateId, TournamentSagaState.READ_TOURNAMENT, unitOfWork);
            this.setTournament(tournamentDTO);
            
        });

        getTournamentStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(tournamentAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep getParticipantStep = new SagaSyncStep("getParticipantStep", () -> {
            UserDto participant = this.tournament.getParticipants().stream().filter(p -> p.getAggregateId().equals(userAggregateId)).findFirst().orElse(null);
            this.setParticipant(participant);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        getParticipantStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(userAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep updateParticipantNameStep = new SagaSyncStep("updateParticipantNameStep", () -> {
            tournamentService.updateUserName(tournamentAggregateId, executionAggregateId, eventVersion, userAggregateId, name,  unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTournamentStep,getParticipantStep)));


        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(getParticipantStep);
        this.workflow.addStep(updateParticipantNameStep);
    }

        

    public void setTournament(SagaTournamentDto tournament) {
        this.tournament = tournament;
    }

    public SagaTournamentDto getTournament() {
        return tournament;
    }


    public void setParticipant(UserDto participant) {
        this.participant = participant;
    }

    public UserDto getParticipant() {
        return this.participant;
    }
}