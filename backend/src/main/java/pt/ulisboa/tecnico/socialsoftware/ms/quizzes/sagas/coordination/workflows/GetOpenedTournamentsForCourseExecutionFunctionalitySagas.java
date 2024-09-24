package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetOpenedTournamentsForCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentDto> openedTournaments;

    

    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetOpenedTournamentsForCourseExecutionFunctionalitySagas(TournamentService tournamentService,SagaUnitOfWorkService unitOfWorkService, 
                                Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getOpenedTournamentsStep = new SyncStep("getOpenedTournamentsStep", () -> {
            List<TournamentDto> openedTournaments = tournamentService.getOpenedTournamentsForCourseExecution(executionAggregateId, unitOfWork);
            this.setOpenedTournaments(openedTournaments);
        });
    
        workflow.addStep(getOpenedTournamentsStep);
    }

    @Override
    public void handleEvents() {

    }

    


    public List<TournamentDto> getOpenedTournaments() {
        return openedTournaments;
    }

    public void setOpenedTournaments(List<TournamentDto> openedTournaments) {
        this.openedTournaments = openedTournaments;
    }
}