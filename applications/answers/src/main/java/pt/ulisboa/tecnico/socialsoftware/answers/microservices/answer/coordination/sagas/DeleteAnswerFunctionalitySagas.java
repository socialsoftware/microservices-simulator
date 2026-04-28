package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.sagas.states.AnswerSagaState;

public class DeleteAnswerFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteAnswerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer answerAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(answerAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer answerAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteAnswerStep = new SagaStep("deleteAnswerStep", () -> {
            unitOfWorkService.verifySagaState(answerAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(AnswerSagaState.READ_ANSWER, AnswerSagaState.UPDATE_ANSWER, AnswerSagaState.DELETE_ANSWER)));
            unitOfWorkService.registerSagaState(answerAggregateId, AnswerSagaState.DELETE_ANSWER, unitOfWork);
            DeleteAnswerCommand cmd = new DeleteAnswerCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), answerAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteAnswerStep);
    }
}
