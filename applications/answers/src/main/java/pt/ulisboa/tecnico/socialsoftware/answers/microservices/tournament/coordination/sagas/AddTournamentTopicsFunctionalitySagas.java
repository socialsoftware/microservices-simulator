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
import java.util.List;

public class AddTournamentTopicsFunctionalitySagas extends WorkflowFunctionality {
    private List<TournamentTopicDto> addedTopicDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddTournamentTopicsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentId, List<TournamentTopicDto> topicDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tournamentId, topicDtos, unitOfWork);
    }

    public void buildWorkflow(Integer tournamentId, List<TournamentTopicDto> topicDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addTopicsStep = new SagaStep("addTopicsStep", () -> {
            AddTournamentTopicsCommand cmd = new AddTournamentTopicsCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), tournamentId, topicDtos);
            List<TournamentTopicDto> addedTopicDtos = (List<TournamentTopicDto>) commandGateway.send(cmd);
            setAddedTopicDtos(addedTopicDtos);
        });

        workflow.addStep(addTopicsStep);
    }
    public List<TournamentTopicDto> getAddedTopicDtos() {
        return addedTopicDtos;
    }

    public void setAddedTopicDtos(List<TournamentTopicDto> addedTopicDtos) {
        this.addedTopicDtos = addedTopicDtos;
    }
}
