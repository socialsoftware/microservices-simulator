package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class RemoveQuestionOptionFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveQuestionOptionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionId, Integer key, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionId, key, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer key, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep removeOptionStep = new SagaStep("removeOptionStep", () -> {
            RemoveQuestionOptionCommand cmd = new RemoveQuestionOptionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId, key);
            commandGateway.send(cmd);
        });

        workflow.addStep(removeOptionStep);
    }
}
