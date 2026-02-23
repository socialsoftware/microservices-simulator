package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.customer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.service.CustomerService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetCustomerByIdFunctionalitySagas extends WorkflowFunctionality {
    private CustomerDto customerDto;
    private final CustomerService customerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetCustomerByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CustomerService customerService, Integer customerAggregateId) {
        this.customerService = customerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(customerAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer customerAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCustomerStep = new SagaSyncStep("getCustomerStep", () -> {
            CustomerDto customerDto = customerService.getCustomerById(customerAggregateId, unitOfWork);
            setCustomerDto(customerDto);
        });

        workflow.addStep(getCustomerStep);
    }
    public CustomerDto getCustomerDto() {
        return customerDto;
    }

    public void setCustomerDto(CustomerDto customerDto) {
        this.customerDto = customerDto;
    }
}
