package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.command.author.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.sagas.states.AuthorSagaState;

public class DeleteAuthorFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteAuthorFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer authorAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(authorAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer authorAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteAuthorStep = new SagaStep("deleteAuthorStep", () -> {
            unitOfWorkService.verifySagaState(authorAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(AuthorSagaState.READ_AUTHOR, AuthorSagaState.UPDATE_AUTHOR, AuthorSagaState.DELETE_AUTHOR)));
            unitOfWorkService.registerSagaState(authorAggregateId, AuthorSagaState.DELETE_AUTHOR, unitOfWork);
            DeleteAuthorCommand cmd = new DeleteAuthorCommand(unitOfWork, ServiceMapping.AUTHOR.getServiceName(), authorAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteAuthorStep);
    }
}
