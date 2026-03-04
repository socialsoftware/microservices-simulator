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
import java.util.List;

public class AddTournamentParticipantsFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentParticipantDto> addedParticipantDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddTournamentParticipantsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentId, List<TournamentParticipantDto> participantDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, participantDtos, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, List<TournamentParticipantDto> participantDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addParticipantsStep = new SagaStep("addParticipantsStep", () -> {
            AddTournamentParticipantsCommand cmd = new AddTournamentParticipantsCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId, participantDtos);
            List<TournamentParticipantDto> addedParticipantDtos = (List<TournamentParticipantDto>) commandGateway.send(cmd);
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
