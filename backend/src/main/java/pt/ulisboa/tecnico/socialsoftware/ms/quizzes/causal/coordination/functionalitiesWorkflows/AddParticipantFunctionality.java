package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalitiesWorkflows;

import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
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

public class AddParticipantFunctionality extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private Tournament tournament;
    private UserDto userDto;
    private Integer userAggregateId;
    private Integer tournamentAggregateId;
    private CausalUnitOfWork unitOfWork;

    private CausalWorkflow workflow;

    private TournamentService tournamentService;
    private CourseExecutionService courseExecutionService;
    private CausalUnitOfWorkService unitOfWorkService;

    private TournamentEventHandling tournamentEventHandling;
    private EventService eventService;
    private String currentStep = "";

    public AddParticipantFunctionality(EventService eventService, TournamentEventHandling tournamentEventHandling, TournamentService tournamentService, CourseExecutionService courseExecutionService, CausalUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
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

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, unitOfWork);
            // by making this call the invariants regarding the course execution and the role of the participant are guaranteed
            UserDto userDto = courseExecutionService.getStudentByExecutionIdAndUserId(tournamentDto.getCourseExecution().getAggregateId(), userAggregateId, unitOfWork);
            TournamentParticipant participant = new TournamentParticipant(userDto);
            tournamentService.addParticipant(tournamentAggregateId, participant, userDto.getRole(), unitOfWork);
        });


        this.workflow.addStep(step);
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

    public void executeWorkflow(CausalUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, CausalUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, CausalUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(CausalUnitOfWork unitOfWork) {
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