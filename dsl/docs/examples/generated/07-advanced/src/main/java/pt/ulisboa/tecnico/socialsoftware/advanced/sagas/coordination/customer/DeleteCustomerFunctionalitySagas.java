package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.customer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.service.CustomerService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteCustomerFunctionalitySagas extends WorkflowFunctionality {
    private final CustomerService customerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteCustomerFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CustomerService customerService, Integer customerAggregateId) {
        this.customerService = customerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(customerAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer customerAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteCustomerStep = new SagaSyncStep("deleteCustomerStep", () -> {
            customerService.deleteCustomer(customerAggregateId, unitOfWork);
        });

        workflow.addStep(deleteCustomerStep);
    }
}
