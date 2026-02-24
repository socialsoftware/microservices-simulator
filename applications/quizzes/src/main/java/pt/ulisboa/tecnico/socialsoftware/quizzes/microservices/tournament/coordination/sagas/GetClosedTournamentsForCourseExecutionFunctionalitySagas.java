package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetClosedTournamentsForCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;

import java.util.List;

public class GetClosedTournamentsForCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentDto> closedTournaments;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetClosedTournamentsForCourseExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                                    Integer executionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getClosedTournamentsStep = new SagaStep("getClosedTournamentsStep", () -> {
            GetClosedTournamentsForCourseExecutionCommand getClosedTournamentsForCourseExecutionCommand = new GetClosedTournamentsForCourseExecutionCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), executionAggregateId);
            List<TournamentDto> closedTournaments = (List<TournamentDto>) commandGateway.send(getClosedTournamentsForCourseExecutionCommand);
            this.setClosedTournaments(closedTournaments);
        });

        workflow.addStep(getClosedTournamentsStep);
    }

    public List<TournamentDto> getClosedTournaments() {
        return closedTournaments;
    }

    public void setClosedTournaments(List<TournamentDto> closedTournaments) {
        this.closedTournaments = closedTournaments;
    }
}