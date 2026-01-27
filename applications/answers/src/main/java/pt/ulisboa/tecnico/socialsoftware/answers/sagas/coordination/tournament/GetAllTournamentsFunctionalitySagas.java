package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllTournamentsFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentDto> tournaments;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllTournamentsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllTournamentsStep = new SagaSyncStep("getAllTournamentsStep", () -> {
            List<TournamentDto> tournaments = tournamentService.getAllTournaments();
            setTournaments(tournaments);
        });

        workflow.addStep(getAllTournamentsStep);

    }

    public List<TournamentDto> getTournaments() {
        return tournaments;
    }

    public void setTournaments(List<TournamentDto> tournaments) {
        this.tournaments = tournaments;
    }
}
