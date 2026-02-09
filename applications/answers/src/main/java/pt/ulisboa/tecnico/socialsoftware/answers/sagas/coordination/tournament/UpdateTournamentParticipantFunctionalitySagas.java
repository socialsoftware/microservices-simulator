package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTournamentParticipantFunctionalitySagas extends WorkflowFunctionality {
    private TournamentParticipantDto updatedParticipantDto;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public UpdateTournamentParticipantFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentId, participantAggregateId, participantDto, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateParticipantStep = new SagaSyncStep("updateParticipantStep", () -> {
            TournamentParticipantDto updatedParticipantDto = tournamentService.updateTournamentParticipant(tournamentId, participantAggregateId, participantDto, unitOfWork);
            setUpdatedParticipantDto(updatedParticipantDto);
        });

        workflow.addStep(updateParticipantStep);
    }
    public TournamentParticipantDto getUpdatedParticipantDto() {
        return updatedParticipantDto;
    }

    public void setUpdatedParticipantDto(TournamentParticipantDto updatedParticipantDto) {
        this.updatedParticipantDto = updatedParticipantDto;
    }
}
