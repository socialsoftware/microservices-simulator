package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.UpdateCourseQuestionCountCommand;

public class UpdateCourseQuestionCountFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateCourseQuestionCountFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer courseExecutionAggregateId, boolean increment,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(courseExecutionAggregateId, increment, unitOfWork);
    }

    private void buildWorkflow(Integer courseExecutionAggregateId, boolean increment, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep step = new SagaStep("updateCourseQuestionCount", () -> {
            UpdateCourseQuestionCountCommand command = new UpdateCourseQuestionCountCommand(unitOfWork,
                    ServiceMapping.EXECUTION.getServiceName(),
                    courseExecutionAggregateId, increment);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
