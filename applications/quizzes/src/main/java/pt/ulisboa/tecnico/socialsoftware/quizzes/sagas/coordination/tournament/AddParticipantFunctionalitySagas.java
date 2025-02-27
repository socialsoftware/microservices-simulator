package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.tournament;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddParticipantFunctionalitySagas extends WorkflowFunctionality {
    
    private UserDto userDto;
    
    private TournamentService tournamentService;
    private CourseExecutionService courseExecutionService;
    private SagaUnitOfWorkService unitOfWorkService;

    public AddParticipantFunctionalitySagas(TournamentService tournamentService, CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentAggregateId, executionAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            this.userDto = courseExecutionService.getStudentByExecutionIdAndUserId(executionAggregateId, userAggregateId, unitOfWork);
        });

        SagaSyncStep addParticipantStep = new SagaSyncStep("addParticipantStep", () -> {
            TournamentParticipant participant = new TournamentParticipant(this.userDto);
            List<SagaAggregate.SagaState> states = new ArrayList<>();
            states.add(TournamentSagaState.IN_UPDATE_TOURNAMENT);
            unitOfWorkService.verifySagaState(tournamentAggregateId, states);
            tournamentService.addParticipant(tournamentAggregateId, participant, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        this.workflow.addStep(getUserStep);
        this.workflow.addStep(addParticipantStep);
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}