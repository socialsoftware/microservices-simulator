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

public class GetTournamentParticipantFunctionalitySagas extends WorkflowFunctionality {
    private TournamentParticipantDto participantDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetTournamentParticipantFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentId, Integer participantAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, participantAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer participantAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getParticipantStep = new SagaStep("getParticipantStep", () -> {
            GetTournamentParticipantCommand cmd = new GetTournamentParticipantCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId, participantAggregateId);
            TournamentParticipantDto participantDto = (TournamentParticipantDto) commandGateway.send(cmd);
            setParticipantDto(participantDto);
        });

        workflow.addStep(getParticipantStep);
    }
    public TournamentParticipantDto getParticipantDto() {
        return participantDto;
    }

    public void setParticipantDto(TournamentParticipantDto participantDto) {
        this.participantDto = participantDto;
    }
}
