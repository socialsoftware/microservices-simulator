package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto updatedTournamentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, TournamentDto tournamentDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentDto, unitOfWork);
    }

    public void buildWorkflow(TournamentDto tournamentDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateTournamentStep = new SagaStep("updateTournamentStep", () -> {
            UpdateTournamentCommand cmd = new UpdateTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentDto);
            TournamentDto updatedTournamentDto = (TournamentDto) commandGateway.send(cmd);
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
