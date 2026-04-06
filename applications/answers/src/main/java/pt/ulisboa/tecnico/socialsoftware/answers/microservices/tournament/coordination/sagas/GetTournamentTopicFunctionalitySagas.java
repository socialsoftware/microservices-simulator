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

public class GetTournamentTopicFunctionalitySagas extends WorkflowFunctionality {
    private TournamentTopicDto topicDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetTournamentTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentId, Integer topicAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTopicStep = new SagaStep("getTopicStep", () -> {
            GetTournamentTopicCommand cmd = new GetTournamentTopicCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId, topicAggregateId);
            TournamentTopicDto topicDto = (TournamentTopicDto) commandGateway.send(cmd);
            setTopicDto(topicDto);
        });

        workflow.addStep(getTopicStep);
    }
    public TournamentTopicDto getTopicDto() {
        return topicDto;
    }

    public void setTopicDto(TournamentTopicDto topicDto) {
        this.topicDto = topicDto;
    }
}
