package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.customer.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.webapi.requestDtos.CreateCustomerRequestDto;

public class CreateCustomerFunctionalitySagas extends WorkflowFunctionality {
    private CustomerDto createdCustomerDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateCustomerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateCustomerRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateCustomerRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createCustomerStep = new SagaStep("createCustomerStep", () -> {
            CreateCustomerCommand cmd = new CreateCustomerCommand(unitOfWork, ServiceMapping.CUSTOMER.getServiceName(), createRequest);
            CustomerDto createdCustomerDto = (CustomerDto) commandGateway.send(cmd);
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
