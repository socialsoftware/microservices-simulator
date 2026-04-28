package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.customer.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.sagas.states.CustomerSagaState;

public class DeleteCustomerFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteCustomerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer customerAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(customerAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer customerAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteCustomerStep = new SagaStep("deleteCustomerStep", () -> {
            unitOfWorkService.verifySagaState(customerAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(CustomerSagaState.READ_CUSTOMER, CustomerSagaState.UPDATE_CUSTOMER, CustomerSagaState.DELETE_CUSTOMER)));
            unitOfWorkService.registerSagaState(customerAggregateId, CustomerSagaState.DELETE_CUSTOMER, unitOfWork);
            DeleteCustomerCommand cmd = new DeleteCustomerCommand(unitOfWork, ServiceMapping.CUSTOMER.getServiceName(), customerAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteCustomerStep);
    }
}
