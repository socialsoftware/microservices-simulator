package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.UpdateCourseQuestionCountCommand;

public class UpdateCourseQuestionCountFunctionalityTCC extends WorkflowFunctionality {
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateCourseQuestionCountFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer courseExecutionAggregateId, boolean increment,
            CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(courseExecutionAggregateId, increment, unitOfWork);
    }

    private void buildWorkflow(Integer courseExecutionAggregateId, boolean increment, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);
        Step step = new Step("updateCourseQuestionCount", () -> {
            UpdateCourseQuestionCountCommand command = new UpdateCourseQuestionCountCommand(unitOfWork,
                    ServiceMapping.EXECUTION.getServiceName(),
                    courseExecutionAggregateId, increment);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
