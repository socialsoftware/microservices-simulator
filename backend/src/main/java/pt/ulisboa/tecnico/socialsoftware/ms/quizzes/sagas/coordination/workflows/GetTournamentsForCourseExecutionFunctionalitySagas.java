package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetTournamentsForCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentDto> tournaments;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetTournamentsForCourseExecutionFunctionalitySagas(TournamentService tournamentService,SagaUnitOfWorkService unitOfWorkService, 
                                Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTournamentsStep = new SagaSyncStep("getTournamentsStep", () -> {
            List<TournamentDto> tournaments = tournamentService.getTournamentsByCourseExecutionId(executionAggregateId, unitOfWork);
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