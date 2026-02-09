package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddTournamentParticipantsFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentParticipantDto> addedParticipantDtos;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AddTournamentParticipantsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, Integer tournamentId, List<TournamentParticipantDto> participantDtos) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(tournamentId, participantDtos, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, List<TournamentParticipantDto> participantDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep addParticipantsStep = new SagaSyncStep("addParticipantsStep", () -> {
            List<TournamentParticipantDto> addedParticipantDtos = tournamentService.addTournamentParticipants(tournamentId, participantDtos, unitOfWork);
            setAddedParticipantDtos(addedParticipantDtos);
        });

        workflow.addStep(addParticipantsStep);
    }
    public List<TournamentParticipantDto> getAddedParticipantDtos() {
        return addedParticipantDtos;
    }

    public void setAddedParticipantDtos(List<TournamentParticipantDto> addedParticipantDtos) {
        this.addedParticipantDtos = addedParticipantDtos;
    }
}
