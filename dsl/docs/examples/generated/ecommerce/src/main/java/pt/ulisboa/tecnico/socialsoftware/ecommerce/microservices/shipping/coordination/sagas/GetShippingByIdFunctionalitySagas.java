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

public class GetShippingByIdFunctionalitySagas extends WorkflowFunctionality {
    private ShippingDto shippingDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetShippingByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer shippingAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(shippingAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer shippingAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getShippingStep = new SagaStep("getShippingStep", () -> {
            GetShippingByIdCommand cmd = new GetShippingByIdCommand(unitOfWork, ServiceMapping.SHIPPING.getServiceName(), shippingAggregateId);
            ShippingDto shippingDto = (ShippingDto) commandGateway.send(cmd);
            setShippingDto(shippingDto);
        });

        workflow.addStep(getShippingStep);
    }
    public ShippingDto getShippingDto() {
        return shippingDto;
    }

    public void setShippingDto(ShippingDto shippingDto) {
        this.shippingDto = shippingDto;
    }
}
