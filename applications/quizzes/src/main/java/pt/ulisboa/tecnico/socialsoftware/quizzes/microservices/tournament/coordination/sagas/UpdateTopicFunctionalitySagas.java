package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.tournament.UpdateTopicCommand;

public class UpdateTopicFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId,
            Integer topicAggregateId, String topicName, Long eventVersion,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tournamentAggregateId, topicAggregateId, topicName, eventVersion, unitOfWork);
    }

    private void buildWorkflow(Integer tournamentAggregateId, Integer topicAggregateId, String topicName,
            Long eventVersion, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep step = new SagaStep("updateTopicStep", () -> {
            UpdateTopicCommand command = new UpdateTopicCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentAggregateId, topicAggregateId, topicName, eventVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
