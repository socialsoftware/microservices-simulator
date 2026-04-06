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

public class AddTournamentTopicFunctionalitySagas extends WorkflowFunctionality {
    private TournamentTopicDto addedTopicDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddTournamentTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentId, Integer topicAggregateId, TournamentTopicDto topicDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, topicAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, Integer topicAggregateId, TournamentTopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addTopicStep = new SagaStep("addTopicStep", () -> {
            AddTournamentTopicCommand cmd = new AddTournamentTopicCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId, topicAggregateId, topicDto);
            TournamentTopicDto addedTopicDto = (TournamentTopicDto) commandGateway.send(cmd);
            setAddedTopicDto(addedTopicDto);
        });

        workflow.addStep(addTopicStep);
    }
    public TournamentTopicDto getAddedTopicDto() {
        return addedTopicDto;
    }

    public void setAddedTopicDto(TournamentTopicDto addedTopicDto) {
        this.addedTopicDto = addedTopicDto;
    }
}
