package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.customer.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateCustomerFunctionalitySagas extends WorkflowFunctionality {
    private CustomerDto updatedCustomerDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateCustomerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CustomerDto customerDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(customerDto, unitOfWork);
    }

    public void buildWorkflow(CustomerDto customerDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateCustomerStep = new SagaStep("updateCustomerStep", () -> {
            UpdateCustomerCommand cmd = new UpdateCustomerCommand(unitOfWork, ServiceMapping.CUSTOMER.getServiceName(), customerDto);
            CustomerDto updatedCustomerDto = (CustomerDto) commandGateway.send(cmd);
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
