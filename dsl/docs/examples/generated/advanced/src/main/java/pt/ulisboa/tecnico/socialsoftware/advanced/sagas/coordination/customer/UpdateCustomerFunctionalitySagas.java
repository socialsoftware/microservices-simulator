package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.customer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.service.CustomerService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateCustomerFunctionalitySagas extends WorkflowFunctionality {
    private CustomerDto updatedCustomerDto;
    private final CustomerService customerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateCustomerFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CustomerService customerService, CustomerDto customerDto) {
        this.customerService = customerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(customerDto, unitOfWork);
    }

    public void buildWorkflow(CustomerDto customerDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateCustomerStep = new SagaSyncStep("updateCustomerStep", () -> {
            CustomerDto updatedCustomerDto = customerService.updateCustomer(customerDto, unitOfWork);
            setUpdatedCustomerDto(updatedCustomerDto);
        });

        workflow.addStep(updateCustomerStep);
    }
    public CustomerDto getUpdatedCustomerDto() {
        return updatedCustomerDto;
    }

    public void setUpdatedCustomerDto(CustomerDto updatedCustomerDto) {
        this.updatedCustomerDto = updatedCustomerDto;
    }
}
