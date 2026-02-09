package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddTournamentParticipantFunctionalitySagas extends WorkflowFunctionality {
    private TournamentParticipantDto addedParticipantDto;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddTournamentParticipantFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentId, participantAggregateId, participantDto, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addParticipantStep = new SagaSyncStep("addParticipantStep", () -> {
            TournamentParticipantDto addedParticipantDto = tournamentService.addTournamentParticipant(tournamentId, participantAggregateId, participantDto, unitOfWork);
            setAddedParticipantDto(addedParticipantDto);
        });

        workflow.addStep(addParticipantStep);
    }
    public TournamentParticipantDto getAddedParticipantDto() {
        return addedParticipantDto;
    }

    public void setAddedParticipantDto(TournamentParticipantDto addedParticipantDto) {
        this.addedParticipantDto = addedParticipantDto;
    }
}
