package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate.sagas.states.QuestionSagaState;

public class DeleteQuestionFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteQuestionStep = new SagaStep("deleteQuestionStep", () -> {
            unitOfWorkService.verifySagaState(questionAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(QuestionSagaState.READ_QUESTION, QuestionSagaState.UPDATE_QUESTION, QuestionSagaState.DELETE_QUESTION)));
            unitOfWorkService.registerSagaState(questionAggregateId, QuestionSagaState.DELETE_QUESTION, unitOfWork);
            DeleteQuestionCommand cmd = new DeleteQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteQuestionStep);
    }
}
