package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.tournament;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;

public class GetClosedTournamentsForCourseExecutionFunctionalityTCC extends WorkflowFunctionality {
    private List<TournamentDto> closedTournaments;
    private final TournamentService tournamentService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public GetClosedTournamentsForCourseExecutionFunctionalityTCC(TournamentService tournamentService,CausalUnitOfWorkService unitOfWorkService, 
                                Integer executionAggregateId, CausalUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            this.closedTournaments = tournamentService.getClosedTournamentsForCourseExecution(executionAggregateId, unitOfWork);
        });
    
        workflow.addStep(step);
    }
    


    public List<TournamentDto> getClosedTournaments() {
        return closedTournaments;
    }

    public void setClosedTournaments(List<TournamentDto> closedTournaments) {
        this.closedTournaments = closedTournaments;
    }
}