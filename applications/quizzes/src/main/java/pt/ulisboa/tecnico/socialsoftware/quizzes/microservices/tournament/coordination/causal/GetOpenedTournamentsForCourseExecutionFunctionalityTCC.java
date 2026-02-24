package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetOpenedTournamentsForCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;

import java.util.List;

public class GetOpenedTournamentsForCourseExecutionFunctionalityTCC extends WorkflowFunctionality {
    private List<TournamentDto> openedTournaments;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetOpenedTournamentsForCourseExecutionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                                                  Integer executionAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            this.openedTournaments = (List<TournamentDto>) commandGateway.send(new GetOpenedTournamentsForCourseExecutionCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), executionAggregateId));
        });

        workflow.addStep(step);
    }

    public List<TournamentDto> getOpenedTournaments() {
        return openedTournaments;
    }

    public void setOpenedTournaments(List<TournamentDto> openedTournaments) {
        this.openedTournaments = openedTournaments;
    }
}
