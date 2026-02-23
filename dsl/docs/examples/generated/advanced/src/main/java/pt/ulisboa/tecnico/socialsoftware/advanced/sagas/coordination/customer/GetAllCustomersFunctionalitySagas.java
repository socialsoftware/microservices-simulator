package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.customer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.service.CustomerService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllCustomersFunctionalitySagas extends WorkflowFunctionality {
    private List<CustomerDto> customers;
    private final CustomerService customerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllCustomersFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CustomerService customerService) {
        this.customerService = customerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllCustomersStep = new SagaSyncStep("getAllCustomersStep", () -> {
            List<CustomerDto> customers = customerService.getAllCustomers(unitOfWork);
            setCustomers(customers);
        });

        workflow.addStep(getAllCustomersStep);
    }
    public List<CustomerDto> getCustomers() {
        return customers;
    }

    public void setCustomers(List<CustomerDto> customers) {
        this.customers = customers;
    }
}
