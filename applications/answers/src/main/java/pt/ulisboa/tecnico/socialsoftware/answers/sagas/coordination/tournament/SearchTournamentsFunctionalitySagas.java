package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class SearchTournamentsFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentDto> searchedTournamentDtos;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public SearchTournamentsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, Boolean cancelled, Integer creatorAggregateId, Integer executionAggregateId, Integer executionCourseAggregateId, Integer quizAggregateId) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(cancelled, creatorAggregateId, executionAggregateId, executionCourseAggregateId, quizAggregateId, unitOfWork);
    }

    public void buildWorkflow(Boolean cancelled, Integer creatorAggregateId, Integer executionAggregateId, Integer executionCourseAggregateId, Integer quizAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep searchTournamentsStep = new SagaSyncStep("searchTournamentsStep", () -> {
            List<TournamentDto> searchedTournamentDtos = tournamentService.searchTournaments(cancelled, creatorAggregateId, executionAggregateId, executionCourseAggregateId, quizAggregateId, unitOfWork);
            setSearchedTournamentDtos(searchedTournamentDtos);
        });

        workflow.addStep(searchTournamentsStep);

    }

    public List<TournamentDto> getSearchedTournamentDtos() {
        return searchedTournamentDtos;
    }

    public void setSearchedTournamentDtos(List<TournamentDto> searchedTournamentDtos) {
        this.searchedTournamentDtos = searchedTournamentDtos;
    }
}
