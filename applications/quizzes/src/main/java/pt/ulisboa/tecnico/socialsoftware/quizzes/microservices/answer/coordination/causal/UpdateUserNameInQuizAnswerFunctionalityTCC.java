package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.UpdateUserNameCommand;

public class UpdateUserNameInQuizAnswerFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateUserNameInQuizAnswerFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer quizAnswerAggregateId, Integer publisherAggregateId, Integer publisherAggregateVersion,
            Integer studentAggregateId, String updatedName,
            CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(quizAnswerAggregateId, publisherAggregateId, publisherAggregateVersion, studentAggregateId,
                updatedName, unitOfWork);
    }

    private void buildWorkflow(Integer quizAnswerAggregateId, Integer publisherAggregateId,
            Integer publisherAggregateVersion,
            Integer studentAggregateId, String updatedName, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        SyncStep step = new SyncStep(() -> {
            UpdateUserNameCommand command = new UpdateUserNameCommand(unitOfWork,
                    ServiceMapping.ANSWER.getServiceName(), quizAnswerAggregateId, publisherAggregateId,
                    publisherAggregateVersion, studentAggregateId, updatedName);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
