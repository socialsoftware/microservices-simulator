package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTournamentParticipantFunctionalitySagas extends WorkflowFunctionality {
    private TournamentParticipantDto updatedParticipantDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTournamentParticipantFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, participantAggregateId, participantDto, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateParticipantStep = new SagaStep("updateParticipantStep", () -> {
            UpdateTournamentParticipantCommand cmd = new UpdateTournamentParticipantCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId, participantAggregateId, participantDto);
            TournamentParticipantDto updatedParticipantDto = (TournamentParticipantDto) commandGateway.send(cmd);
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
