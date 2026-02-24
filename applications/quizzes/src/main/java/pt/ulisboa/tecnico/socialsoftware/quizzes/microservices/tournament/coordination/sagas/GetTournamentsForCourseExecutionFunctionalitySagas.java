package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentsByCourseExecutionIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;

import java.util.List;

public class GetTournamentsForCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentDto> tournaments;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetTournamentsForCourseExecutionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                              Integer executionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTournamentsStep = new SagaStep("getTournamentsStep", () -> {
            GetTournamentsByCourseExecutionIdCommand getTournamentsByCourseExecutionIdCommand = new GetTournamentsByCourseExecutionIdCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), executionAggregateId);
            List<TournamentDto> tournaments = (List<TournamentDto>) commandGateway.send(getTournamentsByCourseExecutionIdCommand);
            this.setTournaments(tournaments);
        });

        workflow.addStep(getTournamentsStep);
    }

    public List<TournamentDto> getTournaments() {
        return tournaments;
    }

    public void setTournaments(List<TournamentDto> tournaments) {
        this.tournaments = tournaments;
    }
}