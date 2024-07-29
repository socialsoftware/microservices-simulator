package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddParticipantFunctionality extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private Tournament tournament;
    private UserDto userDto;
    private Integer userAggregateId;
    private Integer tournamentAggregateId;
    private SagaUnitOfWork unitOfWork;

    private SagaWorkflow workflow;

    private TournamentService tournamentService;
    private CourseExecutionService courseExecutionService;
    private SagaUnitOfWorkService unitOfWorkService;

    private TournamentEventHandling tournamentEventHandling;
    private EventService eventService;
    private String currentStep = "";

    public AddParticipantFunctionality(EventService eventService, TournamentEventHandling tournamentEventHandling, TournamentService tournamentService, CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.eventService = eventService;
        this.tournamentEventHandling = tournamentEventHandling;
        this.tournamentService = tournamentService;
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.tournamentAggregateId = tournamentAggregateId;
        this.userAggregateId = userAggregateId;
        this.unitOfWork = unitOfWork;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getTournamentStep = new SyncStep("getTournamentStep", () -> {
            Tournament tournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
            this.setTournament(tournament);
            this.currentStep = "getTournamentStep";
        });

        SyncStep getUserStep = new SyncStep("getUserStep", () -> {
            UserDto userDto = courseExecutionService.getStudentByExecutionIdAndUserId(
                tournament.getTournamentCourseExecution().getCourseExecutionAggregateId(), userAggregateId, unitOfWork);
            this.setUserDto(userDto);
            this.currentStep = "getUserStep";
        }, new ArrayList<>(Arrays.asList(getTournamentStep)));

        SyncStep addParticipantStep = new SyncStep("addParticipantStep", () -> {
            TournamentParticipant participant = new TournamentParticipant(this.getUserDto());
            tournamentService.addParticipant(tournamentAggregateId, participant, userDto.getRole(), unitOfWork);
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
            } 
        }
    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
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