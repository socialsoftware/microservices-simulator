package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateUserNameCommand;

public class UpdateUserNameFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateUserNameFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService, Integer eventVersion,
            Integer tournamentAggregateId, Integer executionAggregateId,
            Integer userAggregateId, String updatedName, CausalUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(eventVersion, tournamentAggregateId, executionAggregateId, userAggregateId, updatedName,
                unitOfWork);
    }

    private void buildWorkflow(Integer eventVersion, Integer tournamentAggregateId, Integer executionAggregateId,
            Integer userAggregateId, String updatedName, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step(() -> {
            UpdateUserNameCommand command = new UpdateUserNameCommand(unitOfWork,
                    ServiceMapping.TOURNAMENT.getServiceName(),
                    tournamentAggregateId, executionAggregateId, eventVersion, userAggregateId, updatedName);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
