package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTournamentTopicFunctionalitySagas extends WorkflowFunctionality {
    private TournamentTopicDto updatedTopicDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTournamentTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentId, Integer topicAggregateId, TournamentTopicDto topicDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, topicAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer topicAggregateId, TournamentTopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateTopicStep = new SagaStep("updateTopicStep", () -> {
            UpdateTournamentTopicCommand cmd = new UpdateTournamentTopicCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId, topicAggregateId, topicDto);
            TournamentTopicDto updatedTopicDto = (TournamentTopicDto) commandGateway.send(cmd);
            setUpdatedTopicDto(updatedTopicDto);
        });

        workflow.addStep(updateTopicStep);
    }
    public TournamentTopicDto getUpdatedTopicDto() {
        return updatedTopicDto;
    }

    public void setUpdatedTopicDto(TournamentTopicDto updatedTopicDto) {
        this.updatedTopicDto = updatedTopicDto;
    }
}
