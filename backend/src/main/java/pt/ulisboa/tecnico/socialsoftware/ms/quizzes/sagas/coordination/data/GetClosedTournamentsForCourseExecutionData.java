package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetClosedTournamentsForCourseExecutionData extends WorkflowData {
    private List<TournamentDto> closedTournaments;

    private SagaWorkflow workflow;

    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public GetClosedTournamentsForCourseExecutionData(TournamentService tournamentService,SagaUnitOfWorkService unitOfWorkService, 
                                Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getClosedTournamentsStep = new SyncStep(() -> {
            List<TournamentDto> closedTournaments = tournamentService.getClosedTournamentsForCourseExecution(executionAggregateId, unitOfWork);
            this.setClosedTournaments(closedTournaments);
        });
    
        workflow.addStep(getClosedTournamentsStep);
    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
    }


    public List<TournamentDto> getClosedTournaments() {
        return closedTournaments;
    }

    public void setClosedTournaments(List<TournamentDto> closedTournaments) {
        this.closedTournaments = closedTournaments;
    }
}