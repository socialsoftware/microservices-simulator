package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution.RemoveUserCommand;

public class RemoveUserFromCourseExecutionFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveUserFromCourseExecutionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer courseExecutionAggregateId, Integer userAggregateId,
            CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(courseExecutionAggregateId, userAggregateId, unitOfWork);
    }

    private void buildWorkflow(Integer courseExecutionAggregateId, Integer userAggregateId,
            CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step(() -> {
            RemoveUserCommand command = new RemoveUserCommand(unitOfWork,
                    ServiceMapping.EXECUTION.getServiceName(),
                    courseExecutionAggregateId, userAggregateId);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
