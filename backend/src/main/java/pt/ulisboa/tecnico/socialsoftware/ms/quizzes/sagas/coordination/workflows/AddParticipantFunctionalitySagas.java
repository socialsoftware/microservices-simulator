package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;


import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddParticipantFunctionalitySagas extends WorkflowFunctionality {
    
    private SagaTournamentDto tournamentDto;
    private Tournament tournament;
    private UserDto userDto;
    

    private TournamentService tournamentService;
    private CourseExecutionService courseExecutionService;
    private SagaUnitOfWorkService unitOfWorkService;
    private SagaUnitOfWork unitOfWork;

    private EventService eventService;
    private String currentStep = "";

    public AddParticipantFunctionalitySagas(EventService eventService, TournamentEventHandling tournamentEventHandling, TournamentService tournamentService, CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.eventService = eventService;
        this.tournamentService = tournamentService;
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.unitOfWork = unitOfWork;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
            SagaTournamentDto tournamentDto = (SagaTournamentDto) tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);

            //TODO check this if since registerSagaState already blocks there
            if (tournamentDto.getSagaState().equals(GenericSagaState.NOT_IN_SAGA)) {
                unitOfWorkService.registerSagaState(tournamentAggregateId, TournamentSagaState.IN_ADD_PARTICIPANT, unitOfWork);
                setTournamentDto(tournamentDto);
            }
            else {
                switch ((TournamentSagaState) tournamentDto.getSagaState()) {
                    case IN_ADD_PARTICIPANT -> {
                        throw new TutorException(ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA);
                        /* real case
                        while (!tournamentDto.getSagaState().equals(GenericSagaState.NOT_IN_SAGA)) {
                            tournamentDto = (SagaTournamentDto) tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
                            unitOfWorkService.registerSagaState(tournamentAggregateId, TournamentSagaState.IN_ADD_PARTICIPANT, unitOfWork);
                            setTournamentDto(tournamentDto);
                        }
                        */
                    }
                    case IN_UPDATE_TOURNAMENT -> {
                        throw new TutorException(ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA);
                        /* real case
                        while (!tournamentDto.getSagaState().equals(GenericSagaState.NOT_IN_SAGA)) {
                            tournamentDto = (SagaTournamentDto) tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
                            unitOfWorkService.registerSagaState(tournamentAggregateId, TournamentSagaState.IN_ADD_PARTICIPANT, unitOfWork);
                            setTournamentDto(tournamentDto);
                        }
                        */
                    }
                    default -> throw new TutorException(ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA);
                }
            }
        });

        getTournamentStep.registerCompensation(() -> {
            // TODO add more checks like this
            if (tournamentDto != null) {
                unitOfWorkService.registerSagaState(tournamentAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            }
        }, unitOfWork);
    

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            this.userDto = courseExecutionService.getStudentByExecutionIdAndUserId(
                tournamentDto.getCourseExecution().getAggregateId(), userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        SagaSyncStep addParticipantStep = new SagaSyncStep("addParticipantStep", () -> {
            TournamentParticipant participant = new TournamentParticipant(this.getUserDto());
            tournamentService.addParticipant(tournamentAggregateId, participant, userDto.getRole(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(getUserStep);
        this.workflow.addStep(addParticipantStep);
    }

    public void setTournamentDto(SagaTournamentDto tournamentDto) {
        this.tournamentDto = tournamentDto;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public SagaTournamentDto getTournamentDto() {
        return tournamentDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}