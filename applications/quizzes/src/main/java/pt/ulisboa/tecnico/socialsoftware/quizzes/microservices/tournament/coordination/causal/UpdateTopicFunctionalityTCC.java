package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateTopicCommand;

public class UpdateTopicFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTopicFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService, Integer tournamentAggregateId,
            Integer topicAggregateId, String topicName, Integer eventVersion,
            CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tournamentAggregateId, topicAggregateId, topicName, eventVersion, unitOfWork);
    }

    private void buildWorkflow(Integer tournamentAggregateId, Integer topicAggregateId, String topicName,
            Integer eventVersion, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step(() -> {
            UpdateTopicCommand command = new UpdateTopicCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentAggregateId, topicAggregateId, topicName, eventVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
