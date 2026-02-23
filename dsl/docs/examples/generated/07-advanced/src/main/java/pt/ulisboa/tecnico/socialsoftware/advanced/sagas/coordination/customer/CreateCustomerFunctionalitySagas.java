package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.customer;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.service.CustomerService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi.requestDtos.CreateCustomerRequestDto;

public class CreateCustomerFunctionalitySagas extends WorkflowFunctionality {
    private CustomerDto createdCustomerDto;
    private final CustomerService customerService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateCustomerFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, CustomerService customerService, CreateCustomerRequestDto createRequest) {
        this.customerService = customerService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateCustomerRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createCustomerStep = new SagaSyncStep("createCustomerStep", () -> {
            CustomerDto createdCustomerDto = customerService.createCustomer(createRequest, unitOfWork);
            setCreatedCustomerDto(createdCustomerDto);
        });

        workflow.addStep(createCustomerStep);
    }
    public CustomerDto getCreatedCustomerDto() {
        return createdCustomerDto;
    }

    public void setCreatedCustomerDto(CustomerDto createdCustomerDto) {
        this.createdCustomerDto = createdCustomerDto;
    }
}
