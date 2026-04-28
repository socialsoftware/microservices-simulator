package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.sagas.states.TournamentSagaState;

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
            unitOfWorkService.verifySagaState(tournamentDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(TournamentSagaState.READ_TOURNAMENT, TournamentSagaState.UPDATE_TOURNAMENT, TournamentSagaState.DELETE_TOURNAMENT)));
            unitOfWorkService.registerSagaState(tournamentDto.getAggregateId(), TournamentSagaState.UPDATE_TOURNAMENT, unitOfWork);
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
