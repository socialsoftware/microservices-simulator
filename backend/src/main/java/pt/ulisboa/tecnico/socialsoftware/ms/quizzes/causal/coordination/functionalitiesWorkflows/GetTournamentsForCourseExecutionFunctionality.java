package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalitiesWorkflows;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;

public class GetTournamentsForCourseExecutionFunctionality extends WorkflowFunctionality {
    private List<TournamentDto> tournaments;

    private CausalWorkflow workflow;

    private final TournamentService tournamentService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public GetTournamentsForCourseExecutionFunctionality(TournamentService tournamentService,CausalUnitOfWorkService unitOfWorkService, 
                                Integer executionAggregateId, CausalUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.tournaments = tournamentService.getTournamentsByCourseExecutionId(executionAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }

    @Override
    public void handleEvents() {

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

    public List<TournamentDto> getTournaments() {
        return tournaments;
    }

    public void setTournaments(List<TournamentDto> tournaments) {
        this.tournaments = tournaments;
    }
}