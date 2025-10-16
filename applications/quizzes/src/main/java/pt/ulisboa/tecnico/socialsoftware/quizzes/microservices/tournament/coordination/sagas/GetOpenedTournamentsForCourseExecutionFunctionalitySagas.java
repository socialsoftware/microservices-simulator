package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetOpenedTournamentsForCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;

import java.util.List;

public class GetOpenedTournamentsForCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentDto> openedTournaments;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public GetOpenedTournamentsForCourseExecutionFunctionalitySagas(TournamentService tournamentService,
            SagaUnitOfWorkService unitOfWorkService,
            Integer executionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getOpenedTournamentsStep = new SagaSyncStep("getOpenedTournamentsStep", () -> {
            // List<TournamentDto> openedTournaments =
            // tournamentService.getOpenedTournamentsForCourseExecution(executionAggregateId,
            // unitOfWork);
            GetOpenedTournamentsForCourseExecutionCommand getOpenedTournamentsForCourseExecutionCommand = new GetOpenedTournamentsForCourseExecutionCommand(
                    unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), executionAggregateId);
            List<TournamentDto> openedTournaments = (List<TournamentDto>) CommandGateway
                    .send(getOpenedTournamentsForCourseExecutionCommand);
            this.setOpenedTournaments(openedTournaments);
        });

        workflow.addStep(getOpenedTournamentsStep);
    }

    public List<TournamentDto> getOpenedTournaments() {
        return openedTournaments;
    }

    public void setOpenedTournaments(List<TournamentDto> openedTournaments) {
        this.openedTournaments = openedTournaments;
    }
}
