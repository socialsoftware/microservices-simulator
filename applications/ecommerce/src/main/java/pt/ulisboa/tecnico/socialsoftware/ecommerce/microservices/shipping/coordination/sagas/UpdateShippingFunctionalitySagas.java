package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.shipping.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateShippingFunctionalitySagas extends WorkflowFunctionality {
    private ShippingDto updatedShippingDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateShippingFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, ShippingDto shippingDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(shippingDto, unitOfWork);
    }

    public void buildWorkflow(ShippingDto shippingDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateShippingStep = new SagaStep("updateShippingStep", () -> {
            UpdateShippingCommand cmd = new UpdateShippingCommand(unitOfWork, ServiceMapping.SHIPPING.getServiceName(), shippingDto);
            ShippingDto updatedShippingDto = (ShippingDto) commandGateway.send(cmd);
            setUpdatedShippingDto(updatedShippingDto);
        });

        workflow.addStep(updateShippingStep);
    }
    public ShippingDto getUpdatedShippingDto() {
        return updatedShippingDto;
    }

    public void setUpdatedShippingDto(ShippingDto updatedShippingDto) {
        this.updatedShippingDto = updatedShippingDto;
    }
}
