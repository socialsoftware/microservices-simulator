package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddTournamentParticipantFunctionalitySagas extends WorkflowFunctionality {
    private TournamentParticipantDto addedParticipantDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddTournamentParticipantFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, participantAggregateId, participantDto, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer participantAggregateId, TournamentParticipantDto participantDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addParticipantStep = new SagaStep("addParticipantStep", () -> {
            AddTournamentParticipantCommand cmd = new AddTournamentParticipantCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId, participantAggregateId, participantDto);
            TournamentParticipantDto addedParticipantDto = (TournamentParticipantDto) commandGateway.send(cmd);
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
