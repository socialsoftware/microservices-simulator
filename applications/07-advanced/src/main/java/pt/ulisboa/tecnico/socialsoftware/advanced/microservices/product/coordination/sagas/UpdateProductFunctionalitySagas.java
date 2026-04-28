package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.product.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.ProductDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.aggregate.sagas.states.ProductSagaState;

public class UpdateProductFunctionalitySagas extends WorkflowFunctionality {
    private ProductDto updatedProductDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateProductFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, ProductDto productDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(productDto, unitOfWork);
    }

    public void buildWorkflow(ProductDto productDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateProductStep = new SagaStep("updateProductStep", () -> {
            unitOfWorkService.verifySagaState(productDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(ProductSagaState.READ_PRODUCT, ProductSagaState.UPDATE_PRODUCT, ProductSagaState.DELETE_PRODUCT)));
            unitOfWorkService.registerSagaState(productDto.getAggregateId(), ProductSagaState.UPDATE_PRODUCT, unitOfWork);
            UpdateProductCommand cmd = new UpdateProductCommand(unitOfWork, ServiceMapping.PRODUCT.getServiceName(), productDto);
            ProductDto updatedProductDto = (ProductDto) commandGateway.send(cmd);
            setUpdatedProductDto(updatedProductDto);
        });

        workflow.addStep(updateProductStep);
    }
    public ProductDto getUpdatedProductDto() {
        return updatedProductDto;
    }

    public void setUpdatedProductDto(ProductDto updatedProductDto) {
        this.updatedProductDto = updatedProductDto;
    }
}
