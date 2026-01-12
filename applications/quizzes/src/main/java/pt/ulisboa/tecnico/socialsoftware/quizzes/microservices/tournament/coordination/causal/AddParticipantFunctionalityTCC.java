package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class AddParticipantFunctionalityTCC extends WorkflowFunctionality {
    private TournamentDto tournamentDto;
    private Tournament tournament;
    private UserDto userDto;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddParticipantFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId, Integer userAggregateId,
                                          CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            GetTournamentByIdCommand getTournamentByIdCommand = new GetTournamentByIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId);
            TournamentDto tournamentDto = (TournamentDto) commandGateway.send(getTournamentByIdCommand);

            // by making this call the invariants regarding the course execution and the
            // role of the participant are guaranteed
            GetStudentByExecutionIdAndUserIdCommand getStudentByExecutionIdAndUserIdCommand = new GetStudentByExecutionIdAndUserIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), tournamentDto.getCourseExecution().getAggregateId(), userAggregateId);
            UserDto userDto = (UserDto) commandGateway.send(getStudentByExecutionIdAndUserIdCommand);

            AddParticipantCommand addParticipantCommand = new AddParticipantCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentAggregateId, userDto);
            commandGateway.send(addParticipantCommand);
        });

        this.workflow.addStep(step);
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