package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.TournamentSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddParticipantFunctionalitySagas extends WorkflowFunctionality {
    
    private TournamentDto tournamentDto;
    private Tournament tournament;
    private UserDto userDto;
    

    private TournamentService tournamentService;
    private CourseExecutionService courseExecutionService;
    private SagaUnitOfWorkService unitOfWorkService;

    private EventService eventService;
    private String currentStep = "";

    public AddParticipantFunctionalitySagas(EventService eventService, TournamentEventHandling tournamentEventHandling, TournamentService tournamentService, CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.eventService = eventService;
        this.tournamentService = tournamentService;
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentStep = new SagaSyncStep("getTournamentStep", () -> {
            SagaTournament tournament = (SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(tournament, TournamentSagaState.READ_TOURNAMENT, unitOfWork);
            this.setTournament(tournament);
            this.currentStep = "getTournamentStep";
        });

        getTournamentStep.registerCompensation(() -> {
            //TODO check if needed and if needed in other places
            if (tournament != null) {
                unitOfWorkService.registerSagaState((SagaTournament) tournament, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            }
        }, unitOfWork);

        SagaSyncStep getUserStep = new SagaSyncStep("getUserStep", () -> {
            UserDto userDto = courseExecutionService.getStudentByExecutionIdAndUserId(
                tournament.getTournamentCourseExecution().getCourseExecutionAggregateId(), userAggregateId, unitOfWork);
            this.setUserDto(userDto);
            this.currentStep = "getUserStep";
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        SagaSyncStep addParticipantStep = new SagaSyncStep("addParticipantStep", () -> {
            TournamentParticipant participant = new TournamentParticipant(this.getUserDto());
            if (!tournament.getTournamentCreator().getCreatorName().equals("ANONYMOUS")) {
                tournamentService.addParticipant(tournamentAggregateId, participant, userDto.getRole(), unitOfWork);
            }
            this.currentStep = "addParticipantStep";
        }, new ArrayList<>(Arrays.asList(getUserStep)));

        addParticipantStep.registerCompensation(() -> {
            unitOfWork.registerChanged(this.getTournament());
        }, unitOfWork);

        this.workflow.addStep(getTournamentStep);
        this.workflow.addStep(getUserStep);
        this.workflow.addStep(addParticipantStep);
    }

    @Override
    public void handleEvents() {

        if (currentStep.equals("getUserStep") || currentStep.equals("addParticipantStep")) {
        
            Set<EventSubscription> eventSubscriptions = tournament.getEventSubscriptions();
            
            for (EventSubscription eventSubscription: eventSubscriptions) {
                List<? extends Event> eventsToProcess = eventService.getSubscribedEvents(eventSubscription, UpdateStudentNameEvent.class);
                for (Event event: eventsToProcess) {
                    UpdateStudentNameEvent eventToProcess = (UpdateStudentNameEvent) event;
                    this.getUserDto().setName(eventToProcess.getUpdatedName());
                    if (tournament.getTournamentCreator().getCreatorAggregateId().equals(eventToProcess.getStudentAggregateId())) {
                        tournament.getTournamentCreator().setCreatorName(eventToProcess.getUpdatedName());
                    }
                }
                eventsToProcess = eventService.getSubscribedEvents(eventSubscription, AnonymizeStudentEvent.class);
                for (Event event: eventsToProcess) {
                    AnonymizeStudentEvent eventToProcess = (AnonymizeStudentEvent) event;
                    this.getUserDto().setName("ANONYMOUS");
                    this.getUserDto().setUsername("ANONYMOUS");
                    if (tournament.getTournamentCreator().getCreatorAggregateId().equals(eventToProcess.getStudentAggregateId())) {
                        tournament.getTournamentCreator().setCreatorName("ANONYMOUS");
                        tournament.getTournamentCreator().setCreatorUsername("ANONYMOUS");
                    }
                }
                // TODO tournamentRemovedEvent
            } 
        }
    }

    

    public void setTournamentDto(TournamentDto tournamentDto) {
        this.tournamentDto = tournamentDto;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public TournamentDto getTournamentDto() {
        return tournamentDto;
    }

    public void setUserDto(UserDto userDto) {
        this.userDto = userDto;
    }

    public UserDto getUserDto() {
        return userDto;
    }
}