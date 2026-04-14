package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.command.discount.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateDiscountFunctionalitySagas extends WorkflowFunctionality {
    private DiscountDto updatedDiscountDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateDiscountFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, DiscountDto discountDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(discountDto, unitOfWork);
    }

    public void buildWorkflow(DiscountDto discountDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateDiscountStep = new SagaStep("updateDiscountStep", () -> {
            UpdateDiscountCommand cmd = new UpdateDiscountCommand(unitOfWork, ServiceMapping.DISCOUNT.getServiceName(), discountDto);
            DiscountDto updatedDiscountDto = (DiscountDto) commandGateway.send(cmd);
            setUpdatedDiscountDto(updatedDiscountDto);
        });

        workflow.addStep(updateDiscountStep);
    }
    public DiscountDto getUpdatedDiscountDto() {
        return updatedDiscountDto;
    }

    public void setUpdatedDiscountDto(DiscountDto updatedDiscountDto) {
        this.updatedDiscountDto = updatedDiscountDto;
    }
}
