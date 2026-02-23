package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto updatedTournamentDto;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateTournamentFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, TournamentDto tournamentDto) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentDto, unitOfWork);
    }

    public void buildWorkflow(TournamentDto tournamentDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateTournamentStep = new SagaSyncStep("updateTournamentStep", () -> {
            TournamentDto updatedTournamentDto = tournamentService.updateTournament(tournamentDto, unitOfWork);
            setUpdatedTournamentDto(updatedTournamentDto);
        });

        workflow.addStep(updateTournamentStep);
    }
    public TournamentDto getUpdatedTournamentDto() {
        return updatedTournamentDto;
    }

    public void setUpdatedTournamentDto(TournamentDto updatedTournamentDto) {
        this.updatedTournamentDto = updatedTournamentDto;
    }
}
