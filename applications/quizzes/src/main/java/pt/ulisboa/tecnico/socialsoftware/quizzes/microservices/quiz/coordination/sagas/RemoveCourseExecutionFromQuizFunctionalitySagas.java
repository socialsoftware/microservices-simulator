package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.RemoveCourseExecutionCommand;

public class RemoveCourseExecutionFromQuizFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveCourseExecutionFromQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
            Integer quizAggregateId, Integer courseExecutionAggregateId, Integer eventVersion,
            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(quizAggregateId, courseExecutionAggregateId, eventVersion, unitOfWork);
    }

    private void buildWorkflow(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer eventVersion,
            SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep step = new SagaStep("removeCourseExecutionFromQuiz", () -> {
            RemoveCourseExecutionCommand command = new RemoveCourseExecutionCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(),
                    quizAggregateId, courseExecutionAggregateId, eventVersion);
            commandGateway.send(command);
        });
        workflow.addStep(step);
    }
}
